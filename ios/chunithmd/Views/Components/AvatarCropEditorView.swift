import SwiftUI
import UIKit

@MainActor
struct AvatarCropEditorView: View {
    let originalImage: UIImage
    let onSave: (Data) -> Void

    @Environment(\.dismiss) private var dismiss
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    @State private var scale: CGFloat = 1
    @State private var committedScale: CGFloat = 1
    @State private var offset: CGSize = .zero
    @State private var committedOffset: CGSize = .zero
    @State private var rotation: Angle = .zero
    @State private var committedRotation: Angle = .zero
    @State private var didInitializeTransforms = false

    private let maximumScale: CGFloat = 4
    private let guideInset: CGFloat = 12
    private let coveragePadding: CGFloat = 8

    var body: some View {
        GeometryReader { geometry in
            let cropSize = min(geometry.size.width - 32, geometry.size.height * 0.5)

            VStack(spacing: 24) {
                Spacer(minLength: 0)

                VStack(spacing: 14) {
                    Text(tr("裁剪头像"))
                        .font(.headline.bold())
                    Text(tr("拖动、缩放以调整头像"))
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }

                cropCanvas(cropSize: cropSize)

                Button(tr("重置")) {
                    let guideDiameter = cropSize - guideInset * 2
                    let baseSize = baseDisplaySize(for: originalImage.size, cropTargetSize: guideDiameter)
                    resetTransforms(guideDiameter: guideDiameter, baseSize: baseSize)
                }
                .buttonStyle(.bordered)

                Button {
                    saveAvatar(cropSize: cropSize)
                } label: {
                    Text(tr("使用头像"))
                        .font(.headline.bold())
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                }
                .buttonStyle(.borderedProminent)
                .padding(.horizontal, 16)

                Spacer(minLength: 0)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .background(Color(.systemBackground))
        .navigationTitle(tr("裁剪头像"))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button(tr("取消")) { dismiss() }
            }
        }
    }

    @ViewBuilder
    private func cropCanvas(cropSize: CGFloat) -> some View {
        let guideDiameter = cropSize - guideInset * 2
        let baseSize = baseDisplaySize(for: originalImage.size, cropTargetSize: guideDiameter)

        ZStack {
            RoundedRectangle(cornerRadius: 28)
                .fill(Color.black)

            Image(uiImage: originalImage)
                .resizable()
                .frame(width: baseSize.width, height: baseSize.height)
                .scaleEffect(scale)
                .rotationEffect(rotation)
                .offset(offset)
                .gesture(dragGesture(guideDiameter: guideDiameter, baseSize: baseSize))
                .simultaneousGesture(magnificationGesture(guideDiameter: guideDiameter, baseSize: baseSize))
                .simultaneousGesture(rotationGesture(guideDiameter: guideDiameter, baseSize: baseSize))

            Circle()
                .strokeBorder(.white.opacity(0.9), lineWidth: 2)
                .frame(width: guideDiameter, height: guideDiameter)
                .allowsHitTesting(false)
        }
        .onAppear {
            guard didInitializeTransforms == false else { return }
            didInitializeTransforms = true
            resetTransforms(guideDiameter: guideDiameter, baseSize: baseSize, animated: false)
        }
        .frame(width: cropSize, height: cropSize)
        .clipShape(RoundedRectangle(cornerRadius: 28))
        .overlay(
            RoundedRectangle(cornerRadius: 28)
                .strokeBorder(Color.white.opacity(0.12), lineWidth: 1)
        )
        .shadow(color: .black.opacity(0.12), radius: 16, y: 8)
    }

    private func dragGesture(guideDiameter: CGFloat, baseSize: CGSize) -> some Gesture {
        DragGesture()
            .onChanged { value in
                let proposedOffset = CGSize(
                    width: committedOffset.width + value.translation.width,
                    height: committedOffset.height + value.translation.height
                )
                offset = clampedOffset(proposedOffset, guideDiameter: guideDiameter, baseSize: baseSize)
            }
            .onEnded { _ in
                committedOffset = offset
            }
    }

    private func magnificationGesture(guideDiameter: CGFloat, baseSize: CGSize) -> some Gesture {
        MagnifyGesture()
            .onChanged { value in
                let proposedScale = normalizedScale(
                    committedScale * value.magnification, guideDiameter: guideDiameter, baseSize: baseSize)
                scale = proposedScale
                offset = clampedOffset(
                    offset, guideDiameter: guideDiameter, baseSize: baseSize, scale: proposedScale, rotation: rotation)
            }
            .onEnded { value in
                let proposedScale = normalizedScale(
                    committedScale * value.magnification, guideDiameter: guideDiameter, baseSize: baseSize)
                scale = proposedScale
                committedScale = proposedScale
                offset = clampedOffset(
                    offset, guideDiameter: guideDiameter, baseSize: baseSize, scale: proposedScale, rotation: rotation)
                committedOffset = offset
            }
    }

    private func rotationGesture(guideDiameter: CGFloat, baseSize: CGSize) -> some Gesture {
        RotateGesture()
            .onChanged { value in
                let proposedRotation = committedRotation + value.rotation
                rotation = proposedRotation
                offset = clampedOffset(
                    offset, guideDiameter: guideDiameter, baseSize: baseSize, scale: scale, rotation: proposedRotation)
            }
            .onEnded { value in
                let proposedRotation = committedRotation + value.rotation
                rotation = proposedRotation
                committedRotation = proposedRotation
                offset = clampedOffset(
                    offset, guideDiameter: guideDiameter, baseSize: baseSize, scale: scale, rotation: proposedRotation)
                committedOffset = offset
            }
    }

    private func resetTransforms(guideDiameter: CGFloat, baseSize: CGSize, animated: Bool = true) {
        let minScale = minimumScale(for: baseSize, guideDiameter: guideDiameter)
        let updates = {
            scale = minScale
            committedScale = minScale
            offset = .zero
            committedOffset = .zero
            rotation = .zero
            committedRotation = .zero
        }

        if animated && !reduceMotion {
            withAnimation(.snappy, updates)
        } else {
            updates()
        }
    }

    private func baseDisplaySize(for imageSize: CGSize, cropTargetSize: CGFloat) -> CGSize {
        guard imageSize.width > 0, imageSize.height > 0 else {
            return CGSize(width: cropTargetSize, height: cropTargetSize)
        }

        let aspectRatio = imageSize.width / imageSize.height
        let baseSize: CGSize

        if aspectRatio >= 1 {
            baseSize = CGSize(width: cropTargetSize * aspectRatio, height: cropTargetSize)
        } else {
            baseSize = CGSize(width: cropTargetSize, height: cropTargetSize / aspectRatio)
        }

        return baseSize
    }

    private func clampedOffset(
        _ proposedOffset: CGSize,
        guideDiameter: CGFloat,
        baseSize: CGSize,
        scale: CGFloat? = nil,
        rotation: Angle? = nil
    ) -> CGSize {
        let currentScale = scale ?? self.scale
        let currentRotation = rotation ?? self.rotation
        let halfWidth = baseSize.width * currentScale / 2
        let halfHeight = baseSize.height * currentScale / 2
        let guideRadius = guideDiameter / 2 + coveragePadding
        let radians = currentRotation.radians
        let cosine = cos(radians)
        let sine = sin(radians)

        let localX = proposedOffset.width * cosine + proposedOffset.height * sine
        let localY = -proposedOffset.width * sine + proposedOffset.height * cosine

        let horizontalLimit = max(halfWidth - guideRadius, 0)
        let verticalLimit = max(halfHeight - guideRadius, 0)
        let clampedLocalX = min(max(localX, -horizontalLimit), horizontalLimit)
        let clampedLocalY = min(max(localY, -verticalLimit), verticalLimit)

        return CGSize(
            width: clampedLocalX * cosine - clampedLocalY * sine,
            height: clampedLocalX * sine + clampedLocalY * cosine
        )
    }

    private func minimumScale(for baseSize: CGSize, guideDiameter: CGFloat) -> CGFloat {
        let shortestSide = max(min(baseSize.width, baseSize.height), 1)
        let requiredDiameter = guideDiameter + coveragePadding * 2
        return min(max(requiredDiameter / shortestSide, 1), maximumScale)
    }

    private func normalizedScale(_ proposedScale: CGFloat, guideDiameter: CGFloat, baseSize: CGSize) -> CGFloat {
        let minScale = minimumScale(for: baseSize, guideDiameter: guideDiameter)
        return min(max(proposedScale, minScale), maximumScale)
    }

    private func saveAvatar(cropSize: CGFloat) {
        let exportSize: CGFloat = 1024
        let guideDiameter = cropSize - guideInset * 2
        let exportBaseSize = baseDisplaySize(for: originalImage.size, cropTargetSize: exportSize)
        let offsetScale = exportSize / guideDiameter
        let exportOffset = CGSize(width: offset.width * offsetScale, height: offset.height * offsetScale)

        let renderer = ImageRenderer(content:
            Image(uiImage: originalImage)
                .resizable()
                .frame(width: exportBaseSize.width, height: exportBaseSize.height)
                .scaleEffect(scale)
                .rotationEffect(rotation)
                .offset(exportOffset)
                .frame(width: exportSize, height: exportSize)
                .clipped()
        )
        renderer.scale = 1
        guard let imageData = renderer.uiImage?.pngData() else { return }

        onSave(imageData)
        dismiss()
    }
}
