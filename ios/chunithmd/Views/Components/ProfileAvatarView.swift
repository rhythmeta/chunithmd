import SwiftUI

struct ProfileAvatarView: View {
    var data: Data? = nil
    var image: UIImage? = nil
    var body: some View {
        Group {
            if let image = image ?? data.flatMap(UIImage.init(data:)) { Image(uiImage: image).resizable().scaledToFill() }
            else { Image(systemName: "person.crop.circle.fill").resizable().foregroundStyle(.blue.opacity(0.6)).background(.blue.opacity(0.08)) }
        }.clipShape(.circle).accessibilityHidden(true)
    }
}
