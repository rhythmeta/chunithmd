import XCTest

@MainActor
final class NavigationTests: XCTestCase {
    private func capture(_ app: XCUIApplication, name: String) {
        let url = URL.temporaryDirectory.appending(path: "chunithmd-review-\(name).png")
        try? app.screenshot().pngRepresentation.write(to: url)
    }

    private func catalog(appearance: String? = nil) -> XCUIApplication {
        continueAfterFailure = false
        let app = XCUIApplication()
        app.launchArguments = ["-AppleLanguages", "(zh-Hans)", "-AppleLocale", "zh_CN"]
        if let appearance { app.launchArguments += ["-appearance", appearance] }
        app.launch()
        capture(app, name: "home")
        let tab = app.tabBars.buttons["歌曲"]
        if tab.waitForExistence(timeout: 3) { tab.tap() }
        else { app.coordinate(withNormalizedOffset: CGVector(dx: 0.87, dy: 0.935)).tap() }
        XCTAssertTrue(app.navigationBars["歌曲"].waitForExistence(timeout: 10))
        let row = app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH %@", "song-row-")).firstMatch
        let tile = app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH %@", "song-tile-")).firstMatch
        if tile.waitForExistence(timeout: 2) {
            app.buttons["catalog-options"].tap()
        }
        XCTAssertTrue(row.waitForExistence(timeout: 240), "The catalog must finish its first resource download")
        return app
    }

    func testBottomSearchAndCoverReturn() {
        let app = catalog()
        let search = app.searchFields.firstMatch
        XCTAssertTrue(search.exists, "Search tab should expand into a bottom search field")
        search.tap(); search.typeText("Garakuta")
        let row = app.buttons["song-row-Garakuta Doll Play"]
        XCTAssertTrue(row.waitForExistence(timeout: 10))
        row.tap()
        XCTAssertTrue(app.buttons["song-detail-back"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.keyboards.firstMatch.waitForNonExistence(timeout: 5))
        capture(app, name: "detail")
        let detail = XCTAttachment(screenshot: app.screenshot()); detail.name = "Song detail"; detail.lifetime = .keepAlways; add(detail)
        app.buttons["song-detail-back"].tap()
        XCTAssertTrue(row.waitForExistence(timeout: 5))
        XCTAssertEqual(search.value as? String, "Garakuta")
    }

    func testSongDetailCopyActions() {
        let app = catalog()
        let search = app.searchFields.firstMatch
        search.tap(); search.typeText("Garakuta")
        let row = app.buttons["song-row-Garakuta Doll Play"]
        XCTAssertTrue(row.waitForExistence(timeout: 10))
        row.tap()
        XCTAssertTrue(app.navigationBars["Garakuta Doll Play"].waitForExistence(timeout: 5))
        let toast = app.staticTexts["song-copy-toast"]
        for field in ["title", "artist", "bpm", "category", "version", "date"] {
            let copy = app.buttons["song-copy-" + field]
            XCTAssertTrue(copy.exists)
            copy.tap()
            XCTAssertTrue(toast.waitForExistence(timeout: 2))
            XCTAssertEqual(toast.label, "已复制")
            if field == "title" { capture(app, name: "detail-copy-toast") }
            XCTAssertTrue(toast.waitForNonExistence(timeout: 4))
        }
        app.buttons["song-copy-title"].tap()
        app.buttons["song-detail-back"].tap()
        search.tap()
        search.typeText(String(repeating: XCUIKeyboardKey.delete.rawValue, count: 8))
        search.press(forDuration: 1.2)
        let paste = app.menuItems["粘贴"]
        if paste.waitForExistence(timeout: 2) { paste.tap() }
        else { app.buttons["粘贴"].tap() }
        XCTAssertEqual(search.value as? String, "Garakuta Doll Play")
    }

    func testChartCardsAndSettings() {
        let app = catalog()
        capture(app, name: "catalog")
        app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH %@", "song-row-")).firstMatch.tap()
        let card = app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH %@", "chart-card-")).firstMatch
        for _ in 0..<3 where !card.isHittable { app.swipeUp() }
        XCTAssertTrue(card.waitForExistence(timeout: 5))
        card.tap()
        XCTAssertEqual(card.value as? String, "已展开")
        app.swipeUp()
        capture(app, name: "chart")
        app.buttons["song-detail-back"].tap()
        app.terminate()
        app.launch()
        app.tabBars.buttons["设置"].tap()
        XCTAssertTrue(app.buttons["从水鱼查分器导入"].exists)
        capture(app, name: "settings")
    }

    func testFilterChipsAndRange() {
        let app = catalog()
        app.buttons["筛选"].tap()
        let master = app.buttons["MASTER"]
        XCTAssertTrue(master.waitForExistence(timeout: 5))
        master.tap()
        XCTAssertTrue(master.isSelected)
        capture(app, name: "filters")
        app.buttons["重置"].tap()
        XCTAssertFalse(master.isSelected)
        app.buttons["完成"].tap()
    }

    func testChartDetailSectionsAndDarkAppearance() {
        let app = catalog(appearance: "dark")
        app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH %@", "song-row-")).firstMatch.tap()
        XCTAssertTrue(app.buttons["song-detail-back"].waitForExistence(timeout: 5))
        capture(app, name: "detail-dark")
        let card = app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH %@", "chart-card-")).firstMatch
        for _ in 0..<3 where !card.isHittable { app.swipeUp() }
        card.tap()
        app.swipeUp()

        let notes = app.buttons["音符统计"]
        XCTAssertTrue(notes.waitForExistence(timeout: 5))
        notes.tap()
        XCTAssertEqual(notes.value as? String, "已展开")
        XCTAssertTrue(app.staticTexts["TAP"].exists)
        XCTAssertFalse(app.staticTexts["总物量"].exists)
        XCTAssertFalse(app.staticTexts["定数"].exists)
        capture(app, name: "chart-notes-dark")
        notes.tap()

        let rating = app.buttons["分数 → Rating"]
        rating.tap()
        XCTAssertEqual(rating.value as? String, "已展开")
        XCTAssertTrue(app.staticTexts["Rating"].exists)
        XCTAssertTrue(app.staticTexts["差值"].exists)
        XCTAssertTrue(app.staticTexts["↑0.15"].exists)
        capture(app, name: "chart-rating-dark")
        rating.tap()

        let target = app.buttons["SSS"]
        for _ in 0..<3 where !target.isHittable { app.swipeUp() }
        target.tap()
        assertSelected(target)
        capture(app, name: "chart-tolerance-dark")
        let record = app.buttons["录入成绩"]
        for _ in 0..<3 where !record.isHittable { app.swipeUp() }
        record.tap()
        XCTAssertTrue(app.navigationBars["录入成绩"].waitForExistence(timeout: 5))
        app.buttons["取消"].tap()
        app.buttons["加入收藏夹"].tap()
        XCTAssertTrue(app.navigationBars["加入收藏夹"].waitForExistence(timeout: 5))
        app.buttons["完成"].tap()
        app.buttons["song-detail-back"].tap()
    }

    func testHistorySortingAndStatusBadges() {
        let app = catalog()
        app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH %@", "song-row-")).firstMatch.tap()
        let card = app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH %@", "chart-card-")).firstMatch
        for _ in 0..<3 where !card.isHittable { app.swipeUp() }
        card.tap()
        app.swipeUp()
        for score in ["950001", "940001"] {
            let record = app.buttons["录入成绩"]
            for _ in 0..<4 where !record.isHittable { app.swipeUp() }
            record.tap()
            let field = app.textFields["0–1,010,000"]
            XCTAssertTrue(field.waitForExistence(timeout: 5))
            field.tap(); field.typeText(score)
            app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "连击")).firstMatch.tap()
            app.buttons["AJ"].tap()
            app.buttons["保存"].tap()
            XCTAssertTrue(app.buttons["song-detail-back"].waitForExistence(timeout: 5))
        }
        let history = app.buttons["历史成绩"]
        for _ in 0..<4 where !history.isHittable { app.swipeUp() }
        XCTAssertTrue(history.exists)
        history.tap()
        let rows = app.otherElements.matching(NSPredicate(format: "identifier BEGINSWITH %@", "score-history-row-"))
        XCTAssertTrue(rows.firstMatch.waitForExistence(timeout: 5))
        XCTAssertTrue(rows.firstMatch.staticTexts["940,001"].exists)
        XCTAssertTrue(rows.firstMatch.staticTexts["AJ"].exists)
        app.buttons["分数"].tap()
        assertSelected(app.buttons["分数"])
        let scores = rows.allElementsBoundByIndex.compactMap { row in
            row.staticTexts.allElementsBoundByIndex.map(\.label).first {
                $0.range(of: "^[0-9]{1,3}(,[0-9]{3})+$", options: .regularExpression) != nil
            }.flatMap { Int($0.replacingOccurrences(of: ",", with: "")) }
        }
        XCTAssertGreaterThanOrEqual(scores.count, 2)
        XCTAssertEqual(scores, scores.sorted(by: >))
        capture(app, name: "history-score-badges")
        app.buttons["时间"].tap()
        assertSelected(app.buttons["时间"])
        XCTAssertTrue(rows.firstMatch.staticTexts["940,001"].exists)
        for _ in 0..<2 {
            rows.firstMatch.buttons["删除成绩"].tap()
            app.buttons["删除"].tap()
        }
        app.buttons["song-detail-back"].tap()
    }

    func testSortMenuMatchesReferenceAndPersists() {
        var app = catalog()
        app.buttons["catalog-sort"].tap()
        XCTAssertTrue(app.buttons["默认"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.buttons["版本/日期"].exists)
        XCTAssertTrue(app.buttons["难度"].exists)
        XCTAssertFalse(app.buttons["按标题"].exists)
        app.buttons["版本/日期"].tap()
        app.buttons["catalog-sort"].tap()
        assertSelected(app.buttons["版本/日期"])
        let ascending = app.buttons["升序"]
        if ascending.exists {
            ascending.tap()
        } else {
            app.buttons["降序"].tap()
            app.buttons["catalog-sort"].tap()
            app.buttons["升序"].tap()
        }
        app.buttons["catalog-sort"].tap()
        assertSelected(app.buttons["版本/日期"])
        XCTAssertTrue(app.buttons["降序"].exists)
        app.terminate()
        app = catalog()
        app.buttons["catalog-sort"].tap()
        assertSelected(app.buttons["版本/日期"])
        XCTAssertTrue(app.buttons["降序"].exists)
        capture(app, name: "sort")
        app.buttons["难度"].tap()
        app.buttons["catalog-sort"].tap()
        assertSelected(app.buttons["难度"])
        app.buttons["默认"].tap()
        app.buttons["catalog-sort"].tap()
        assertSelected(app.buttons["默认"])
        app.buttons["降序"].tap()
    }

    private func assertSelected(_ element: XCUIElement, file: StaticString = #filePath, line: UInt = #line) {
        let selected = XCTNSPredicateExpectation(predicate: NSPredicate(format: "selected == true"), object: element)
        XCTAssertEqual(XCTWaiter.wait(for: [selected], timeout: 5), .completed, file: file, line: line)
    }

    func testSharedLocalizationInEnglishJapaneseAndTraditionalChinese() {
        continueAfterFailure = false
        let languages = [
            ("en", "Home", "Settings", "Songs", "Default", "Version/Date", "Difficulty", "Player profiles"),
            ("ja", "ホーム", "設定", "楽曲", "デフォルト", "バージョン/日付", "難易度", "プレイヤープロフィール"),
            ("zh-Hant", "首頁", "設定", "歌曲", "預設", "版本/日期", "難度", "玩家檔案")
        ]
        for (language, home, settings, songs, defaultSort, version, difficulty, profiles) in languages {
            let app = XCUIApplication()
            app.launchArguments = ["-AppleLanguages", "(\(language))", "-AppleLocale", language]
            app.launch()
            XCTAssertTrue(app.navigationBars[home].waitForExistence(timeout: 10))
            app.tabBars.buttons[settings].tap()
            XCTAssertTrue(app.buttons[profiles].waitForExistence(timeout: 5))
            app.tabBars.buttons[songs].tap()
            XCTAssertTrue(app.buttons["catalog-sort"].waitForExistence(timeout: 10))
            app.buttons["catalog-sort"].tap()
            XCTAssertTrue(app.buttons[defaultSort].waitForExistence(timeout: 5))
            XCTAssertTrue(app.buttons[version].exists)
            XCTAssertTrue(app.buttons[difficulty].exists)
            capture(app, name: "sort-\(language)")
            app.terminate()
        }
    }

    func testPinchGridAndCoverReturn() {
        let app = catalog()
        let options = app.buttons["catalog-options"]
        XCTAssertTrue(options.waitForExistence(timeout: 5))
        options.tap()
        let tile = app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH %@", "song-tile-")).firstMatch
        XCTAssertTrue(tile.waitForExistence(timeout: 5))
        app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH %@", "song-tile-")).element(boundBy: 7).pinch(withScale: 0.5, velocity: -1)
        let before = tile.frame.width
        capture(app, name: "grid-five-columns")
        app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH %@", "song-tile-")).element(boundBy: 7).pinch(withScale: 1.8, velocity: 1)
        XCTAssertGreaterThan(tile.frame.width, before * 1.3)
        capture(app, name: "grid-three-columns")
        let grid = XCTAttachment(screenshot: app.screenshot()); grid.name = "Three column grid"; grid.lifetime = .keepAlways; add(grid)
        tile.tap()
        XCTAssertTrue(app.buttons["song-detail-back"].waitForExistence(timeout: 5))
        app.buttons["song-detail-back"].tap()
        XCTAssertTrue(tile.waitForExistence(timeout: 5))
        options.tap()
        XCTAssertTrue(app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH %@", "song-row-")).firstMatch.waitForExistence(timeout: 5))
    }
    func testInteractiveBackCanCancelAndFinish() {
        let app = catalog()
        let row = app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH %@", "song-row-")).firstMatch
        row.tap()
        let back = app.buttons["song-detail-back"]
        XCTAssertTrue(back.waitForExistence(timeout: 5))
        let start = app.coordinate(withNormalizedOffset: CGVector(dx: 0.015, dy: 0.3))
        start.press(forDuration: 0.1, thenDragTo: app.coordinate(withNormalizedOffset: CGVector(dx: 0.15, dy: 0.3)), withVelocity: .slow, thenHoldForDuration: 0.5)
        XCTAssertTrue(back.exists)
        start.press(forDuration: 0.1, thenDragTo: app.coordinate(withNormalizedOffset: CGVector(dx: 0.8, dy: 0.3)), withVelocity: .slow, thenHoldForDuration: 0.2)
        XCTAssertTrue(row.waitForExistence(timeout: 5))
        XCTAssertFalse(back.exists)
    }

    func testConstantPosterRenders() {
        continueAfterFailure = false
        let app = XCUIApplication()
        app.launch()
        app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "定数表,")).firstMatch.tap()
        let share = app.buttons["分享定数表"]
        XCTAssertTrue(share.waitForExistence(timeout: 10))
        share.tap()
        XCTAssertTrue(app.images["chart-poster-preview"].firstMatch.waitForExistence(timeout: 120))
        XCTAssertTrue(app.buttons["poster-share"].exists)
    }

}
