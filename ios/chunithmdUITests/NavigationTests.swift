import XCTest

@MainActor
final class NavigationTests: XCTestCase {
    private func capture(_ app: XCUIApplication, name: String) {
        let url = URL.temporaryDirectory.appending(path: "chunithmd-review-\(name).png")
        try? app.screenshot().pngRepresentation.write(to: url)
    }

    private func catalog() -> XCUIApplication {
        continueAfterFailure = false
        let app = XCUIApplication()
        app.launchArguments = ["-AppleLanguages", "(zh-Hans)", "-AppleLocale", "zh_CN"]
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
            app.buttons["列表"].tap()
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
        app.buttons["网格"].tap()
        options.tap()
        app.buttons["5 列"].tap()
        let tile = app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH %@", "song-tile-")).firstMatch
        XCTAssertTrue(tile.waitForExistence(timeout: 5))
        let before = tile.frame.width
        app.scrollViews.firstMatch.pinch(withScale: 1.8, velocity: 1)
        XCTAssertGreaterThan(tile.frame.width, before * 1.3)
        let grid = XCTAttachment(screenshot: app.screenshot()); grid.name = "Three column grid"; grid.lifetime = .keepAlways; add(grid)
        tile.tap()
        XCTAssertTrue(app.buttons["song-detail-back"].waitForExistence(timeout: 5))
        app.buttons["song-detail-back"].tap()
        XCTAssertTrue(tile.waitForExistence(timeout: 5))
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
