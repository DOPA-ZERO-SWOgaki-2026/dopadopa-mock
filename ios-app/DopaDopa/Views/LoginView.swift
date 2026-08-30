import SwiftUI

/// index.html の #loginScreen 相当。
struct LoginView: View {
    @EnvironmentObject private var appState: AppState
    @State private var username: String = ""
    @FocusState private var isFieldFocused: Bool

    var body: some View {
        ZStack {
            Theme.background.ignoresSafeArea()

            VStack(spacing: 20) {
                Circle()
                    .fill(Theme.ringGradient)
                    .frame(width: 56, height: 56)
                    .overlay(
                        Text("D")
                            .font(.title.bold())
                            .foregroundColor(.white)
                    )

                Text("WELCOME")
                    .font(.caption.weight(.bold))
                    .foregroundColor(Theme.primary)
                    .tracking(2)

                Text("DopaDopa")
                    .font(.system(size: 30, weight: .bold))
                    .foregroundColor(Theme.ink)

                Text("ユーザーネームを登録して、あなたのデトックス記録を開始しましょう。")
                    .font(.subheadline)
                    .foregroundColor(Theme.subtleInk)
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)

                VStack(alignment: .leading, spacing: 8) {
                    Text("ユーザーネーム")
                        .font(.footnote.weight(.semibold))
                        .foregroundColor(Theme.subtleInk)

                    TextField("例: yuki", text: $username)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .focused($isFieldFocused)
                        .foregroundColor(Theme.ink)
                        .tint(Theme.primary)
                        .padding(12)
                        .background(Theme.background)
                        .clipShape(RoundedRectangle(cornerRadius: Theme.controlCorner))
                        .overlay(
                            RoundedRectangle(cornerRadius: Theme.controlCorner)
                                .stroke(Color.black.opacity(0.08), lineWidth: 1)
                        )
                        .submitLabel(.done)
                        .onSubmit(submit)
                }

                Button(action: submit) {
                    Text("アカウント作成")
                        .font(.headline)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(Theme.primary)
                        .clipShape(RoundedRectangle(cornerRadius: Theme.controlCorner))
                }
                .disabled(username.trimmingCharacters(in: .whitespaces).isEmpty)
            }
            .padding(28)
            .background(Theme.cardBackground)
            .clipShape(RoundedRectangle(cornerRadius: Theme.cardCorner))
            .shadow(color: .black.opacity(0.06), radius: 24, y: 12)
            .padding(.horizontal, 24)
        }
    }

    private func submit() {
        appState.login(username: username)
    }
}

#Preview {
    LoginView().environmentObject(AppState.shared)
}
