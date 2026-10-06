#!/usr/bin/env python3
"""Audit Swift source keys against the shared catalog, including interpolated strings."""
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CJK = re.compile(r'[\u3400-\u9fff]')
# Direct SwiftUI copy must use the shared catalog even when written in English.
# These are language-invariant game terms, brands, units and score notation.
UI_COPY_CONTEXT = re.compile(
    r'\b(?:Text|Label|Button|Toggle|Picker|Section|TextField|SecureField|'
    r'ProgressView|ContentUnavailableView|LabeledContent|NavigationLink|Tab|'
    r'Menu|Link|ShareLink|SharePreview|navigationTitle|accessibilityLabel|'
    r'accessibilityHint|accessibilityValue|alert|confirmationDialog)\(\s*(?:verbatim:\s*)?$'
    r'|\b(?:title|subtitle|prompt|message|label):\s*$'
)
INVARIANT_WORDS = frozenset({
    'Rating', 'B', 'N', 'B30', 'N20', 'NEW', 'BEST', 'Best', 'R', 'Lv', 'BPM',
    'CHUNITHM', 'chunithmd', 'JUSTICE', 'ATTACK', 'MISS',
    'Spirit', 'Tribute', 'Legend', 'FC', 'AJ', 'AJC',
    'Otogame', 'YouTube', 'Bilibili', 'SHA',
})


def literal(source, start):
    """Return end, literal chunks, and interpolation expressions for a Swift string."""
    chunks, arguments = [], []
    index = chunk_start = start + 1
    while index < len(source):
        if source[index] == '"':
            chunks.append(source[chunk_start:index])
            return index + 1, chunks, arguments
        if source.startswith('\\(', index):
            chunks.append(source[chunk_start:index])
            expression_start = index + 2
            index, depth = expression_start, 1
            while depth:
                if source[index] == '"':
                    index = literal(source, index)[0]
                    continue
                if source[index] == '(':
                    depth += 1
                elif source[index] == ')':
                    depth -= 1
                index += 1
            arguments.append(source[expression_start:index - 1])
            chunk_start = index
        elif source[index] == '\\':
            index += 2
        else:
            index += 1
    raise ValueError('Unterminated Swift string')


def strings(source):
    index = 0
    while index < len(source):
        if source.startswith('//', index):
            end = source.find('\n', index)
            index = len(source) if end < 0 else end + 1
        elif source.startswith('/*', index):
            index = source.index('*/', index + 2) + 2
        elif source.startswith('#"""', index) or source.startswith('"""', index):
            raw = source.startswith('#', index)
            delimiter = '"""#' if raw else '"""'
            content_start = index + (4 if raw else 3)
            end = source.index(delimiter, content_start)
            assert not CJK.search(source[content_start:end]), 'Localize multiline UI text with a source key'
            index = end + len(delimiter)
        elif source[index] == '"':
            end, chunks, arguments = literal(source, index)
            yield index, end, chunks, arguments
            index = end
        else:
            index += 1


def check_source(source, catalog, context):
    for start, _, chunks, arguments in strings(source):
        location = f'{context}:{source.count(chr(10), 0, start) + 1}'
        is_key = re.search(r'\btr\(\s*$', source[:start]) is not None
        if is_key:
            assert not arguments, f'Interpolated localization key in {location}'
            key = json.loads('"' + chunks[0] + '"')
            assert key in catalog, f'Missing catalog key in {location}: {key}'
        elif any(CJK.search(chunk) for chunk in chunks):
            raise AssertionError(f'Unlocalized Swift text in {location}: {chunks}')
        elif UI_COPY_CONTEXT.search(source[:start]):
            # Decode escapes before checking words (e.g. a line break before R).
            decoded = ''.join(
                json.loads('"' + chunk + '"') for chunk in chunks
            )
            words = {word for word in re.findall(r'[^\W_]+', decoded)
                     if any(character.isalpha() for character in word)}
            assert not words - INVARIANT_WORDS, f'Unlocalized Swift UI text in {location}: {chunks}'
        for argument in arguments:
            check_source(argument, catalog, context)


def validate(catalog):
    for path in (ROOT / 'ios/chunithmd').rglob('*.swift'):
        check_source(path.read_text(), catalog, path.relative_to(ROOT))


if __name__ == '__main__':
    validate(json.loads((ROOT / 'localization/strings.json').read_text()))
    print('iOS source keys use the shared localization catalog.')
