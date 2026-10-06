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

    func testCloudAccountSignedOutActionsAndSummary() {
        continueAfterFailure = false
        let app = XCUIApplication()
        for appearance in ["light", "dark"] {
            app.launchArguments = ["-AppleLanguages", "(zh-Hans)", "-AppleLocale", "zh_CN", "-appearance", appearance]
            app.launch()
            XCTAssertTrue(app.tabBars.buttons["设置"].waitForExistence(timeout: 20))
            app.tabBars.buttons["设置"].tap()
            app.buttons["settings-cloud-account"].tap()
            XCTAssertTrue(app.navigationBars["Rhythmeta"].waitForExistence(timeout: 5))
            XCTAssertTrue(app.staticTexts["登录 Rhythmeta 账号以备份和恢复数据。"].exists)
            XCTAssertTrue(app.staticTexts["Rhythmeta 账号可用于 maimaid 和 chunithmd，备份需要手动创建。"].exists)
            for action in ["cloud-login", "cloud-register", "cloud-forgot"] {
                XCTAssertTrue(app.buttons[action].waitForExistence(timeout: 10))
                XCTAssertTrue(app.buttons[action].isEnabled)
            }
            XCTAssertFalse(app.textFields.firstMatch.exists)
            capture(app, name: "cloud-account-" + appearance)
            app.navigationBars.buttons.firstMatch.tap()
            XCTAssertTrue(app.navigationBars["设置"].waitForExistence(timeout: 5))
            app.terminate()
        }
    }

    func testSettingsSectionsThemeHealthAndResourcesNavigation() {
        continueAfterFailure = false
        let app = XCUIApplication()
        for appearance in ["light", "dark"] {
            app.launchArguments = ["-AppleLanguages", "(zh-Hans)", "-AppleLocale", "zh_CN", "-appearance", appearance]
            app.launch()
            XCTAssertTrue(app.tabBars.buttons["设置"].waitForExistence(timeout: 20))
            app.tabBars.buttons["设置"].tap()
            XCTAssertTrue(app.navigationBars["设置"].waitForExistence(timeout: 5))
            XCTAssertTrue(app.staticTexts["用户管理"].exists)
            XCTAssertTrue(app.staticTexts["数据同步"].exists)
            XCTAssertTrue(app.staticTexts["成绩同步"].exists)
            capture(app, name: "settings-" + appearance)
            let resources = app.buttons["settings-resources"]
            XCTAssertTrue(resources.exists)
            resources.tap()
            XCTAssertTrue(app.navigationBars["静态数据"].waitForExistence(timeout: 5))
            let checkResources = app.buttons["static-resources-check"]
            XCTAssertTrue(checkResources.waitForExistence(timeout: 30))
            XCTAssertTrue(app.staticTexts["更新操作"].exists)
            XCTAssertFalse(app.staticTexts["SHA-256"].exists)
            XCTAssertFalse(app.buttons["完成"].exists)
            capture(app, name: "settings-resources-" + appearance)
            checkResources.tap()
            XCTAssertTrue(checkResources.waitForExistence(timeout: 30))
            app.swipeDown()
            XCTAssertTrue(checkResources.waitForExistence(timeout: 30))
            app.navigationBars.buttons.firstMatch.tap()
            app.swipeUp()
            XCTAssertTrue(app.staticTexts["后端状态"].waitForExistence(timeout: 5))
            XCTAssertTrue(app.staticTexts["版本"].exists)
            let health = app.staticTexts.matching(NSPredicate(format: "label IN %@", ["可用", "不可用"])).firstMatch
            XCTAssertTrue(health.waitForExistence(timeout: 10))
            capture(app, name: "settings-about-" + appearance)
            app.buttons["settings-theme"].tap()
            XCTAssertTrue(app.buttons["跟随系统"].waitForExistence(timeout: 5))
            XCTAssertTrue(app.buttons["浅色"].exists)
            XCTAssertTrue(app.buttons["深色"].exists)
            capture(app, name: "settings-theme-" + appearance)
            app.terminate()
        }
    }

    func testCollectionsImportPreviewsSortGridPickerAndRename() {
        continueAfterFailure = false
        let app = XCUIApplication()
        app.launchArguments = ["-AppleLanguages", "(zh-Hans)", "-AppleLocale", "zh_CN", "-appearance", "light"]
        app.launch()
        XCTAssertTrue(app.tabBars.buttons["收藏"].waitForExistence(timeout: 20))
        app.tabBars.buttons["收藏"].tap()
        for title in ["UI 收藏夹检查", "UI 重命名检查"] {
            let previous = app.buttons.matching(NSPredicate(format: "label CONTAINS %@", title)).firstMatch
            if previous.exists {
                previous.swipeLeft()
                app.buttons["删除"].firstMatch.tap()
                XCTAssertTrue(app.buttons["collection-delete-confirm"].firstMatch.waitForExistence(timeout: 5))
                app.buttons["collection-delete-confirm"].firstMatch.tap()
                XCTAssertTrue(previous.waitForNonExistence(timeout: 5))
            }
        }
        app.buttons["collections-actions"].tap()
        capture(app, name: "collections-menu")
        XCTAssertFalse(app.buttons["导入收藏夹"].exists)
        XCTAssertTrue(app.buttons["从剪贴板导入"].exists)
        app.coordinate(withNormalizedOffset: CGVector(dx: 0.1, dy: 0.4)).tap()
        app.open(URL(string: "chunithmd://collection/CHMD1.4xIK9VR4NmXbi-n9T5fsfLa44dn8pULSXDwvZ297MXP-i91dL_bMFmIuLkmRYstNLC5JLcIhmVpRkFpUIiTGxe5YlJiemJuJqglDHKIeAA")!)
        let folder = app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "UI 收藏夹检查")).firstMatch
        XCTAssertTrue(folder.waitForExistence(timeout: 5))
        XCTAssertFalse(app.navigationBars["导入收藏夹"].exists)
        XCTAssertFalse(app.buttons["导入收藏夹"].exists)
        capture(app, name: "collections-list")
        folder.tap()
        XCTAssertTrue(app.buttons["collection-layout"].waitForExistence(timeout: 5))
        if app.buttons["collection-layout"].label == "列表" { app.buttons["collection-layout"].tap() }
        let rows = app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH %@", "collection-chart-"))
        XCTAssertTrue(rows.firstMatch.waitForExistence(timeout: 5))
        capture(app, name: "collection-songs")
        app.buttons["collection-sort"].tap()
        capture(app, name: "collection-sort")
        app.buttons["定数"].tap()
        XCTAssertTrue(rows.firstMatch.waitForExistence(timeout: 5))
        app.buttons["collection-layout"].tap()
        let grid = app.scrollViews.firstMatch
        XCTAssertTrue(app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH %@", "song-tile-")).firstMatch.waitForExistence(timeout: 5))
        capture(app, name: "collection-grid")
        grid.pinch(withScale: 2, velocity: 2)
        capture(app, name: "collection-grid-zoom")
        app.buttons["collection-layout"].tap()
        rows.firstMatch.tap()
        XCTAssertTrue(app.buttons["song-detail-back"].waitForExistence(timeout: 5))
        let add = app.buttons["加入收藏夹"].firstMatch
        for _ in 0..<8 where !add.isHittable { app.swipeUp() }
        XCTAssertTrue(add.isHittable)
        add.tap()
        XCTAssertTrue(app.navigationBars["加入收藏夹"].waitForExistence(timeout: 5))
        capture(app, name: "collection-picker")
        let choice = app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH %@", "collection-picker-")).firstMatch
        choice.tap()
        app.buttons["取消"].tap()
        add.tap()
        XCTAssertTrue(app.navigationBars["加入收藏夹"].waitForExistence(timeout: 5))
        app.buttons["完成"].tap()
        app.buttons["song-detail-back"].tap()
        XCTAssertEqual(rows.count, 4)
        app.buttons["collection-actions"].tap()
        capture(app, name: "collection-actions")
        app.buttons["重命名收藏夹"].tap()
        let name = app.alerts.textFields.firstMatch
        name.tap()
        name.typeText(String(repeating: XCUIKeyboardKey.delete.rawValue, count: 40) + "UI 重命名检查")
        app.alerts.buttons["保存"].tap()
        XCTAssertTrue(app.navigationBars["UI 重命名检查"].waitForExistence(timeout: 5))
        app.navigationBars.buttons.firstMatch.tap()
        app.terminate()
        app.launchArguments = ["-AppleLanguages", "(zh-Hans)", "-AppleLocale", "zh_CN", "-appearance", "dark"]
        app.launch()
        app.tabBars.buttons["收藏"].tap()
        let renamed = app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "UI 重命名检查")).firstMatch
        XCTAssertTrue(renamed.waitForExistence(timeout: 10))
        capture(app, name: "collections-dark")
        renamed.tap()
        XCTAssertTrue(rows.firstMatch.waitForExistence(timeout: 5))
        capture(app, name: "collection-songs-dark")
        app.navigationBars.buttons.firstMatch.tap()
        renamed.swipeLeft()
        capture(app, name: "collections-swipe")
        app.buttons["删除"].firstMatch.tap()
        XCTAssertTrue(app.buttons["collection-delete-confirm"].firstMatch.waitForExistence(timeout: 5))
        capture(app, name: "collection-delete-confirm")
        app.buttons["collection-delete-confirm"].firstMatch.tap()
        XCTAssertTrue(renamed.waitForNonExistence(timeout: 5))
        capture(app, name: "collections-empty")
    }

    func testCommunityBoardGroupsDeadlinesRefreshAndSongNavigation() {
        continueAfterFailure = false
        let app = XCUIApplication()
        app.launchArguments = ["-AppleLanguages", "(zh-Hans)", "-AppleLocale", "zh_CN", "-appearance", "light"]
        app.launch()
        XCTAssertTrue(app.buttons["home-profile"].waitForExistence(timeout: 20))
        app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "社区别名")).firstMatch.tap()
        let headers = app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH %@", "community-song-"))
        let loaded = headers.firstMatch.waitForExistence(timeout: 30)
        capture(app, name: "community-loaded")
        XCTAssertTrue(loaded)
        XCTAssertTrue(app.staticTexts.matching(NSPredicate(format: "label BEGINSWITH %@", "截止 ")).firstMatch.exists)
        let support = app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH %@", "community-support-"))
        let oppose = app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH %@", "community-oppose-"))
        XCTAssertTrue(support.firstMatch.label.contains("支持"))
        XCTAssertTrue(oppose.firstMatch.label.contains("反对"))
        XCTAssertLessThan(support.firstMatch.frame.maxX, oppose.firstMatch.frame.minX)
        if app.buttons["community-login"].exists {
            XCTAssertFalse(support.firstMatch.isEnabled)
            app.buttons["community-login"].tap()
            XCTAssertTrue(app.navigationBars["Rhythmeta"].waitForExistence(timeout: 5))
            app.navigationBars.buttons.firstMatch.tap()
        }
        capture(app, name: "community-board")
        headers.firstMatch.tap()
        XCTAssertTrue(app.buttons["song-detail-back"].waitForExistence(timeout: 5))
        app.buttons["song-detail-back"].tap()
        app.swipeDown()
        XCTAssertTrue(headers.firstMatch.waitForExistence(timeout: 30))
        capture(app, name: "community-refreshed")
        app.swipeUp()
        capture(app, name: "community-groups")
        app.terminate()
        app.launchArguments = ["-AppleLanguages", "(zh-Hans)", "-AppleLocale", "zh_CN", "-appearance", "dark"]
        app.launch()
        app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "社区别名")).firstMatch.tap()
        XCTAssertTrue(headers.firstMatch.waitForExistence(timeout: 30))
        capture(app, name: "community-dark")
    }

    func testPlateProgressMenusGridAndNavigation() {
        continueAfterFailure = false
        let app = XCUIApplication()
        app.launchArguments = ["-AppleLanguages", "(zh-Hans)", "-AppleLocale", "zh_CN", "-appearance", "light"]
        app.launch()
        XCTAssertTrue(app.buttons["home-profile"].waitForExistence(timeout: 20))
        app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "牌子进度")).firstMatch.tap()
        let tiles = app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH %@", "plate-tile-"))
        XCTAssertTrue(tiles.firstMatch.waitForExistence(timeout: 30))
        XCTAssertTrue(tiles.firstMatch.label.contains("MASTER"))
        capture(app, name: "plate-progress")
        let version = app.buttons["plate-version"]
        let difficulty = app.buttons["plate-difficulty"]
        let kind = app.buttons["plate-kind"]
        XCTAssertEqual(version.frame.height, difficulty.frame.height, accuracy: 1)
        XCTAssertEqual(kind.frame.height, difficulty.frame.height, accuracy: 1)
        version.tap()
        capture(app, name: "plate-versions")
        for _ in 0..<4 where !app.buttons["MATE"].exists {
            app.coordinate(withNormalizedOffset: CGVector(dx: 0.3, dy: 0.83))
                .press(forDuration: 0.05, thenDragTo: app.coordinate(withNormalizedOffset: CGVector(dx: 0.3, dy: 0.45)))
        }
        app.buttons["MATE"].tap()
        XCTAssertTrue(tiles.firstMatch.waitForExistence(timeout: 5))
        kind.tap()
        capture(app, name: "plate-kinds")
        app.buttons["Legend · AJ"].tap()
        XCTAssertTrue(app.staticTexts["Legend of MATE"].waitForExistence(timeout: 5))
        let played = app.buttons["plate-tile-零號車輛:std:master"]
        XCTAssertTrue(played.waitForExistence(timeout: 5))
        XCTAssertTrue((played.value as? String ?? "").contains("AJC"))
        capture(app, name: "plate-completed")
        played.tap()
        XCTAssertTrue(app.buttons["song-detail-back"].waitForExistence(timeout: 5))
        app.buttons["song-detail-back"].tap()
        app.buttons["plate-filter"].tap()
        app.buttons.matching(NSPredicate(format: "label == %@ AND identifier != %@", "只看未完成", "plate-filter")).firstMatch.tap()
        XCTAssertFalse(played.exists)
        capture(app, name: "plate-remaining")
        difficulty.tap()
        capture(app, name: "plate-difficulties")
        app.buttons["EXPERT"].tap()
        XCTAssertTrue(tiles.firstMatch.waitForExistence(timeout: 5))
        XCTAssertTrue(tiles.firstMatch.label.contains("EXPERT"))
        capture(app, name: "plate-expert")
        app.terminate()
        app.launchArguments = ["-AppleLanguages", "(zh-Hans)", "-AppleLocale", "zh_CN", "-appearance", "dark"]
        app.launch()
        app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "牌子进度")).firstMatch.tap()
        XCTAssertTrue(tiles.firstMatch.waitForExistence(timeout: 20))
        capture(app, name: "plate-dark")
    }

    func testConstantTableSectionsFiltersBadgesAndExport() {
        continueAfterFailure = false
        let app = XCUIApplication()
        app.launchArguments = ["-AppleLanguages", "(zh-Hans)", "-AppleLocale", "zh_CN", "-appearance", "light"]
        app.launch()
        XCTAssertTrue(app.buttons["home-profile"].waitForExistence(timeout: 20))
        app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "定数表")).firstMatch.tap()
        let tiles = app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH %@", "constant-tile-"))
        XCTAssertTrue(tiles.firstMatch.waitForExistence(timeout: 30))
        XCTAssertEqual(tiles.firstMatch.value as? String ?? "", "")
        XCTAssertLessThan(app.buttons["constant-table-filter"].frame.width, 60)
        capture(app, name: "constant-table")
        app.buttons["constant-table-level"].tap()
        XCTAssertTrue(app.buttons["15~16"].waitForExistence(timeout: 5))
        capture(app, name: "constant-table-levels")
        app.buttons["15~16"].tap()
        XCTAssertTrue(tiles.firstMatch.waitForExistence(timeout: 5))
        capture(app, name: "constant-table-high-level")
        let scoresToggle = app.switches["constant-table-scores"]
        scoresToggle.coordinate(withNormalizedOffset: CGVector(dx: 0.94, dy: 0.5)).tap()
        XCTAssertEqual(scoresToggle.value as? String, "1")
        let played = app.buttons["constant-tile-零號車輛:std:master"]
        for _ in 0..<16 where !played.isHittable { app.swipeUp() }
        XCTAssertTrue(played.isHittable)
        capture(app, name: "constant-table-badges")
        XCTAssertTrue((played.value as? String ?? "").contains("AJC"))
        XCTAssertTrue((played.value as? String ?? "").contains("金 FC"))
        played.tap()
        XCTAssertTrue(app.buttons["song-detail-back"].waitForExistence(timeout: 5))
        app.buttons["song-detail-back"].tap()
        for _ in 0..<16 where !app.buttons["constant-table-export"].isHittable { app.swipeDown() }
        app.buttons["constant-table-filter"].tap()
        XCTAssertTrue(app.navigationBars["筛选歌曲"].waitForExistence(timeout: 5))
        XCTAssertFalse(app.staticTexts["难度与定数"].exists)
        XCTAssertFalse(app.staticTexts["谱面类型"].exists)
        app.switches["仅显示喜爱歌曲"].tap()
        capture(app, name: "constant-table-filter")
        app.buttons["完成"].tap()
        app.buttons["constant-table-filter"].tap()
        app.buttons["重置"].tap()
        app.buttons["完成"].tap()
        XCTAssertTrue(tiles.firstMatch.waitForExistence(timeout: 5))
        app.buttons["constant-table-level"].tap()
        app.buttons["15~16"].tap()
        app.buttons["constant-table-export"].tap()
        let preview = app.images["chart-poster-preview"].firstMatch
        XCTAssertTrue(preview.waitForExistence(timeout: 120))
        XCTAssertTrue(app.buttons["poster-share"].exists)
        capture(app, name: "constant-table-export")
        app.buttons["完成"].tap()
        app.terminate()
        app.launchArguments = ["-AppleLanguages", "(zh-Hans)", "-AppleLocale", "zh_CN", "-appearance", "dark"]
        app.launch()
        app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "定数表")).firstMatch.tap()
        XCTAssertTrue(tiles.firstMatch.waitForExistence(timeout: 20))
        capture(app, name: "constant-table-dark")
    }

    func testScoreQueryLayoutFiltersSearchAndNavigation() {
        continueAfterFailure = false
        let app = XCUIApplication()
        app.launchArguments = ["-AppleLanguages", "(zh-Hans)", "-AppleLocale", "zh_CN", "-appearance", "light"]
        app.launch()
        XCTAssertTrue(app.buttons["home-profile"].waitForExistence(timeout: 20))
        app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "成绩查询")).firstMatch.tap()
        let rows = app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH %@", "score-query-row-"))
        XCTAssertTrue(app.buttons["score-query-layout"].waitForExistence(timeout: 30))
        if app.buttons["score-query-layout"].label == "列表视图" { app.buttons["score-query-layout"].tap() }
        XCTAssertTrue(rows.firstMatch.waitForExistence(timeout: 5))
        XCTAssertTrue(rows.firstMatch.label.contains("1,010,000"))
        XCTAssertTrue(rows.firstMatch.label.contains("AJC"))
        XCTAssertTrue(rows.firstMatch.label.contains("17.35"))
        capture(app, name: "score-query-list")
        for identifier in ["score-query-layout", "score-query-sort", "score-query-filter"] {
            XCTAssertLessThan(app.buttons[identifier].frame.width, 60)
        }
        app.buttons["score-query-sort"].tap()
        XCTAssertTrue(app.buttons["Rating"].exists)
        XCTAssertTrue(app.buttons["分数"].exists)
        XCTAssertTrue(app.buttons["定数"].exists)
        capture(app, name: "score-query-sort")
        app.buttons["分数"].tap()
        app.buttons["score-query-sort"].tap()
        let descending = app.buttons["降序"]
        if descending.exists { descending.tap() } else { app.buttons["升序"].tap() }

        app.buttons["score-query-filter"].tap()
        XCTAssertTrue(app.navigationBars["筛选"].waitForExistence(timeout: 5))
        XCTAssertFalse(app.staticTexts["谱面类型"].exists)
        app.buttons["D"].tap()
        capture(app, name: "score-query-filters")
        app.buttons["完成"].tap()
        XCTAssertTrue(app.staticTexts["没有符合条件的成绩"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.otherElements["score-query-stats"].exists)
        capture(app, name: "score-query-empty")
        app.buttons["score-query-filter"].tap()
        app.buttons["重置筛选"].tap()
        app.buttons["MASTER"].tap()
        app.buttons["SSS+"].tap()
        app.buttons["AJC"].tap()
        app.buttons["金 FC"].tap()
        capture(app, name: "score-query-filters-selected")
        app.buttons["完成"].tap()
        XCTAssertTrue(rows.firstMatch.waitForExistence(timeout: 5))
        rows.firstMatch.tap()
        XCTAssertTrue(app.buttons["song-detail-back"].waitForExistence(timeout: 5))
        app.buttons["song-detail-back"].tap()
        XCTAssertTrue(rows.firstMatch.waitForExistence(timeout: 5))

        app.buttons["score-query-layout"].tap()
        let tiles = app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH %@", "score-query-tile-"))
        XCTAssertTrue(tiles.firstMatch.waitForExistence(timeout: 5))
        capture(app, name: "score-query-grid")
        tiles.firstMatch.pinch(withScale: 0.55, velocity: -1)
        let smallWidth = tiles.firstMatch.frame.width
        tiles.firstMatch.pinch(withScale: 2, velocity: 1)
        capture(app, name: "score-query-grid-zoom")
        XCTAssertGreaterThan(tiles.firstMatch.frame.width, smallWidth * 1.4)
        let search = app.searchFields.firstMatch
        if !search.exists && app.buttons["搜索"].exists { app.buttons["搜索"].tap() }
        search.tap()
        search.typeText("zzzz-no-matching-song")
        XCTAssertTrue(app.staticTexts["没有符合条件的成绩"].waitForExistence(timeout: 5))
        capture(app, name: "score-query-search")
        if app.buttons["关闭"].exists { app.buttons["关闭"].tap() } else { app.buttons["取消"].tap() }
        XCTAssertTrue(tiles.firstMatch.waitForExistence(timeout: 5))

        app.terminate()
        app.launchArguments = ["-AppleLanguages", "(zh-Hans)", "-AppleLocale", "zh_CN", "-appearance", "dark"]
        app.launch()
        app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "成绩查询")).firstMatch.tap()
        XCTAssertTrue(app.buttons["score-query-layout"].waitForExistence(timeout: 20))
        if app.buttons["score-query-layout"].label == "列表视图" { app.buttons["score-query-layout"].tap() }
        XCTAssertTrue(rows.firstMatch.waitForExistence(timeout: 5))
        capture(app, name: "score-query-dark")
    }

    func testRecommendationRowsScopeAndNavigation() {
        continueAfterFailure = false
        let app = XCUIApplication()
        app.launchArguments = ["-AppleLanguages", "(zh-Hans)", "-AppleLocale", "zh_CN", "-appearance", "light"]
        app.launch()
        XCTAssertTrue(app.buttons["home-profile"].waitForExistence(timeout: 20))
        app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "吃分推荐")).firstMatch.tap()
        let rows = app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH %@", "recommendation-row-"))
        XCTAssertTrue(rows.firstMatch.waitForExistence(timeout: 120))
        let firstNew = rows.firstMatch.identifier
        XCTAssertTrue(rows.firstMatch.staticTexts.matching(NSPredicate(format: "label MATCHES %@", "[+][0-9]+[.][0-9]{2}")).firstMatch.exists)
        XCTAssertTrue(rows.firstMatch.staticTexts.matching(NSPredicate(format: "label BEGINSWITH %@", "目标 ")).firstMatch.exists)
        capture(app, name: "recommendations-new")
        XCTAssertLessThan(app.buttons["recommendation-scope"].frame.width, 60)
        app.buttons["recommendation-scope"].tap()
        capture(app, name: "recommendations-scope-menu")
        app.buttons["旧曲推荐"].tap()
        XCTAssertTrue(rows.firstMatch.waitForExistence(timeout: 5))
        XCTAssertNotEqual(rows.firstMatch.identifier, firstNew)
        capture(app, name: "recommendations-old")
        let initialIDs = Set(rows.allElementsBoundByIndex.map(\.identifier))
        let list = app.collectionViews["recommendation-list"]
        for _ in 0..<4 { list.swipeUp() }
        XCTAssertTrue(rows.allElementsBoundByIndex.contains { !initialIDs.contains($0.identifier) })
        rows.allElementsBoundByIndex.first { $0.isHittable }!.tap()
        XCTAssertTrue(app.buttons["song-detail-back"].waitForExistence(timeout: 5))
        app.buttons["song-detail-back"].tap()
        XCTAssertTrue(app.buttons["recommendation-scope"].label.contains("旧曲推荐"))
        for _ in 0..<5 { list.swipeDown() }
        XCTAssertTrue(rows.firstMatch.waitForExistence(timeout: 5))
        app.terminate()
        app.launchArguments = ["-AppleLanguages", "(zh-Hans)", "-AppleLocale", "zh_CN", "-appearance", "dark"]
        app.launch()
        app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "吃分推荐")).firstMatch.tap()
        XCTAssertTrue(rows.firstMatch.waitForExistence(timeout: 20))
        capture(app, name: "recommendations-dark")
    }

    func testProfileListBadgesAndActions() {
        continueAfterFailure = false
        let app = XCUIApplication()
        app.launchArguments = ["-AppleLanguages", "(zh-Hans)", "-AppleLocale", "zh_CN"]
        app.launch()
        XCTAssertTrue(app.buttons["home-profile"].waitForExistence(timeout: 20))
        app.tabBars.buttons["设置"].tap()
        app.buttons["用户档案"].tap()
        XCTAssertTrue(app.navigationBars["用户档案"].waitForExistence(timeout: 5))
        let rows = app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH %@", "profile-row-"))
        let original = rows.allElementsBoundByIndex.first { ($0.value as? String) == "当前档案" }!
        XCTAssertTrue(original.staticTexts["日服"].exists || original.staticTexts["国际服"].exists || original.staticTexts["国服"].exists)
        capture(app, name: "profiles")
        original.swipeLeft()
        XCTAssertTrue(app.buttons["编辑"].exists)
        XCTAssertFalse(app.buttons["删除"].exists)
        app.buttons["编辑"].tap()
        XCTAssertTrue(app.navigationBars["编辑档案"].waitForExistence(timeout: 5))
        app.buttons["取消"].tap()

        let temporaryName = "UI Profile 56F8B0AF"
        let temporary = rows.matching(NSPredicate(format: "label CONTAINS %@", temporaryName)).firstMatch
        if temporary.exists {
            temporary.swipeLeft()
            app.buttons["删除"].tap()
            XCTAssertTrue(app.staticTexts["删除后本地档案信息将无法恢复。"].waitForExistence(timeout: 5))
            app.buttons["删除"].tap()
            XCTAssertTrue(temporary.waitForNonExistence(timeout: 5))
        }
        app.buttons["新建档案"].tap()
        app.textFields["profile-name"].tap()
        app.textFields["profile-name"].typeText(temporaryName)
        app.buttons["保存"].tap()
        XCTAssertTrue(temporary.waitForExistence(timeout: 5))
        capture(app, name: "profiles-multiple")
        temporary.tap()
        XCTAssertEqual(temporary.value as? String, "当前档案")
        original.tap()
        XCTAssertEqual(original.value as? String, "当前档案")
        temporary.swipeLeft()
        capture(app, name: "profiles-swipe")
        app.buttons["删除"].tap()
        XCTAssertTrue(app.staticTexts["删除后本地档案信息将无法恢复。"].waitForExistence(timeout: 5))
        capture(app, name: "profiles-delete-confirmation")
        app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.7)).tap()
        XCTAssertTrue(temporary.exists)
        temporary.swipeLeft()
        app.buttons["删除"].tap()
        XCTAssertTrue(app.staticTexts["删除后本地档案信息将无法恢复。"].waitForExistence(timeout: 5))
        app.buttons["删除"].tap()
        XCTAssertTrue(temporary.waitForNonExistence(timeout: 5))
        XCTAssertEqual(original.value as? String, "当前档案")
    }

    func testProfileEditorLayoutAndDraftCancellation() {
        continueAfterFailure = false
        let app = XCUIApplication()
        app.launchArguments = ["-AppleLanguages", "(zh-Hans)", "-AppleLocale", "zh_CN"]
        app.launch()
        let profile = app.buttons["home-profile"]
        XCTAssertTrue(profile.waitForExistence(timeout: 20))
        profile.tap()
        XCTAssertTrue(app.navigationBars["编辑档案"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["基本信息"].exists)
        XCTAssertTrue(app.buttons["清除头像"].exists)
        XCTAssertTrue(app.buttons["保存"].isEnabled)
        capture(app, name: "profile-editor")
        let title = app.textFields["profile-title"]
        let originalTitle = title.value as? String
        title.tap(); title.typeText(" Draft")
        app.buttons["取消"].tap()
        profile.tap()
        XCTAssertEqual(app.textFields["profile-title"].value as? String, originalTitle)
        app.buttons["选择头像"].tap()
        capture(app, name: "profile-photo-picker")
        // The system photo picker must dismiss back into the same unsaved form.
        let photos = app.navigationBars["照片"]
        XCTAssertTrue(photos.waitForExistence(timeout: 10))
        photos.buttons["取消"].tap()
        XCTAssertTrue(photos.waitForNonExistence(timeout: 5))
        XCTAssertTrue(app.navigationBars["编辑档案"].waitForExistence(timeout: 5))
        app.buttons["取消"].tap()
        app.tabBars.buttons["设置"].tap()
        app.buttons["用户档案"].tap()
        app.buttons["新建档案"].tap()
        XCTAssertFalse(app.buttons["保存"].isEnabled)
        let name = app.textFields["profile-name"]
        name.tap(); name.typeText("   ")
        XCTAssertFalse(app.buttons["保存"].isEnabled)
        name.typeText("Temporary")
        XCTAssertTrue(app.buttons["保存"].isEnabled)
        app.buttons["取消"].tap()
        XCTAssertFalse(app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "Temporary")).firstMatch.exists)
    }

    func testProfileAvatarCropAndClear() throws {
        continueAfterFailure = false
        let app = XCUIApplication()
        app.launchArguments = ["-AppleLanguages", "(zh-Hans)", "-AppleLocale", "zh_CN"]
        app.launch()
        XCTAssertTrue(app.buttons["home-profile"].waitForExistence(timeout: 20))
        app.buttons["home-profile"].tap()
        app.buttons["选择头像"].tap()
        XCTAssertTrue(app.navigationBars["照片"].waitForExistence(timeout: 10))
        let loaded = XCTNSPredicateExpectation(predicate: NSPredicate { _, _ in
            app.collectionViews.cells.allElementsBoundByIndex.contains { $0.isHittable }
        }, object: nil)
        guard XCTWaiter.wait(for: [loaded], timeout: 15) == .completed,
              let photo = app.collectionViews.cells.allElementsBoundByIndex.first(where: { $0.isHittable }) else {
            throw XCTSkip("Simulator photo library has no selectable images")
        }
        capture(app, name: "profile-photo-library")
        photo.tap()
        XCTAssertTrue(app.navigationBars["裁剪头像"].waitForExistence(timeout: 10))
        capture(app, name: "profile-avatar-crop")
        app.buttons["重置"].tap()
        app.buttons["使用头像"].tap()
        XCTAssertTrue(app.navigationBars["裁剪头像"].waitForNonExistence(timeout: 5))
        XCTAssertTrue(app.buttons["清除头像"].isEnabled)
        capture(app, name: "profile-avatar-preview")
        app.buttons["清除头像"].tap()
        XCTAssertFalse(app.buttons["清除头像"].isEnabled)
        app.buttons["取消"].tap()
    }

    func testRandomDrawFiltersAndCancellation() {
        let app = catalog()
        let search = app.searchFields.firstMatch
        search.tap(); search.typeText("no-song-matches-this-search\n")
        app.tabBars.buttons["首页"].tap()
        app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "随机歌曲")).firstMatch.tap()
        let draw = app.buttons["random-draw"]
        XCTAssertTrue(draw.waitForExistence(timeout: 5))
        XCTAssertTrue(draw.isEnabled, "Random songs must ignore catalog search")
        capture(app, name: "random-ready")
        draw.tap()
        app.segmentedControls.buttons["一次 4 首"].tap()
        XCTAssertFalse(app.staticTexts["抽选结果"].exists)
        draw.tap()
        XCTAssertTrue(app.staticTexts["抽选结果"].waitForExistence(timeout: 8))
        let rows = app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH %@", "song-row-"))
        XCTAssertEqual(rows.count, 4)
        capture(app, name: "random-four")
        let before = draw.frame.minY
        app.scrollViews["random-page"].swipeUp()
        XCTAssertLessThan(draw.frame.minY, before - 20, "The controls and results must scroll together")
        capture(app, name: "random-page-scrolled")
        app.scrollViews["random-page"].swipeDown()
        draw.doubleTap()
        capture(app, name: "random-skip")
        XCTAssertTrue(app.staticTexts["抽选结果"].waitForExistence(timeout: 3))
        XCTAssertEqual(rows.count, 4)
        rows.firstMatch.tap()
        XCTAssertTrue(app.buttons["song-detail-back"].waitForExistence(timeout: 5))
        app.buttons["song-detail-back"].tap()
        app.buttons["random-filter"].tap()
        app.switches["仅显示喜爱歌曲"].tap()
        app.buttons["完成"].tap()
        XCTAssertFalse(app.staticTexts["抽选结果"].exists)
        app.buttons["random-filter"].tap()
        XCTAssertEqual(app.switches["仅显示喜爱歌曲"].value as? String, "1")
        app.buttons["完成"].tap()
        app.navigationBars.buttons.firstMatch.tap()
        app.tabBars.buttons["歌曲"].tap()
        XCTAssertEqual(app.searchFields.firstMatch.value as? String, "no-song-matches-this-search")
        app.buttons["关闭"].tap()
        XCTAssertTrue(app.buttons["筛选"].waitForExistence(timeout: 5))
        app.buttons["筛选"].tap()
        XCTAssertEqual(app.switches["仅显示喜爱歌曲"].value as? String, "0")
        app.buttons["完成"].tap()
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
        let record = app.buttons["记录成绩"]
        for _ in 0..<3 where !record.isHittable { app.swipeUp() }
        record.tap()
        XCTAssertTrue(app.navigationBars["记录成绩"].waitForExistence(timeout: 5))
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
            let record = app.buttons["记录成绩"]
            for _ in 0..<4 where !record.isHittable { app.swipeUp() }
            record.tap()
            let field = app.textFields["0–1,010,000"]
            XCTAssertTrue(field.waitForExistence(timeout: 5))
            field.tap(); field.typeText(score)
            app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "COMBO 状态")).firstMatch.tap()
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
            rows.firstMatch.buttons["删除成绩记录"].tap()
            app.buttons["删除"].tap()
        }
        app.buttons["song-detail-back"].tap()
    }

    func testSortMenuMatchesReferenceAndPersists() {
        var app = catalog()
        app.buttons["catalog-sort"].tap()
        XCTAssertTrue(app.buttons["默认顺序"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.buttons["版本 / 发行日期"].exists)
        XCTAssertTrue(app.buttons["难度"].exists)
        XCTAssertFalse(app.buttons["按标题"].exists)
        app.buttons["版本 / 发行日期"].tap()
        app.buttons["catalog-sort"].tap()
        assertSelected(app.buttons["版本 / 发行日期"])
        let ascending = app.buttons["升序"]
        if ascending.exists {
            ascending.tap()
        } else {
            app.buttons["降序"].tap()
            app.buttons["catalog-sort"].tap()
            app.buttons["升序"].tap()
        }
        app.buttons["catalog-sort"].tap()
        assertSelected(app.buttons["版本 / 发行日期"])
        XCTAssertTrue(app.buttons["降序"].exists)
        app.terminate()
        app = catalog()
        app.buttons["catalog-sort"].tap()
        assertSelected(app.buttons["版本 / 发行日期"])
        XCTAssertTrue(app.buttons["降序"].exists)
        capture(app, name: "sort")
        app.buttons["难度"].tap()
        app.buttons["catalog-sort"].tap()
        assertSelected(app.buttons["难度"])
        app.buttons["默认顺序"].tap()
        app.buttons["catalog-sort"].tap()
        assertSelected(app.buttons["默认顺序"])
        app.buttons["降序"].tap()
    }

    private func assertSelected(_ element: XCUIElement, file: StaticString = #filePath, line: UInt = #line) {
        let selected = XCTNSPredicateExpectation(predicate: NSPredicate(format: "selected == true"), object: element)
        XCTAssertEqual(XCTWaiter.wait(for: [selected], timeout: 5), .completed, file: file, line: line)
    }

    func testSharedLocalizationInEnglishJapaneseAndTraditionalChinese() {
        continueAfterFailure = false
        let languages = [
            ("en", "Home", "Settings", "Songs", "Default order", "Version / release date", "Difficulty", "Profiles", "Song, artist, alias…"),
            ("ja", "ホーム", "設定", "楽曲", "標準の順序", "バージョン / 配信日", "難易度", "プロフィール", "曲名・アーティスト・別名…"),
            ("zh-Hant", "首頁", "設定", "歌曲", "預設順序", "版本 / 發行日期", "難度", "個人檔案", "曲名、演出者、別名…")
        ]
        for (language, home, settings, songs, defaultSort, version, difficulty, profiles, searchPrompt) in languages {
            let app = XCUIApplication()
            app.launchArguments = ["-AppleLanguages", "(\(language))", "-AppleLocale", language]
            app.launch()
            XCTAssertTrue(app.navigationBars[home].waitForExistence(timeout: 10))
            app.tabBars.buttons[settings].tap()
            XCTAssertTrue(app.buttons[profiles].waitForExistence(timeout: 5))
            app.tabBars.buttons[songs].tap()
            XCTAssertTrue(app.buttons["catalog-sort"].waitForExistence(timeout: 10))
            XCTAssertTrue(app.searchFields[searchPrompt].waitForExistence(timeout: 5))
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
        app.launchArguments = ["-AppleLanguages", "(zh-Hans)", "-AppleLocale", "zh_CN", "-appearance", "light"]
        app.launch()
        XCTAssertTrue(app.buttons["home-profile"].waitForExistence(timeout: 20))
        app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "定数表")).firstMatch.tap()
        let share = app.buttons["constant-table-export"]
        XCTAssertTrue(share.waitForExistence(timeout: 10))
        share.tap()
        XCTAssertTrue(app.images["chart-poster-preview"].firstMatch.waitForExistence(timeout: 120))
        XCTAssertTrue(app.buttons["poster-share"].exists)
    }

    func testBestTableSettingsRowsAndShare() {
        continueAfterFailure = false
        let app = XCUIApplication()
        app.launchArguments = ["-AppleLanguages", "(zh-Hans)", "-AppleLocale", "zh_CN", "-appearance", "light"]
        app.launch()
        app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "查看 Best 50 成绩表")).firstMatch.tap()
        XCTAssertTrue(app.staticTexts["best-rating"].waitForExistence(timeout: 10))
        XCTAssertTrue(app.staticTexts["容量设置"].exists)
        capture(app, name: "best-table")

        let best = app.textFields["best-capacity"]
        let original = best.value as! String
        best.doubleTap()
        best.typeText("1")
        app.buttons["完成"].tap()
        XCTAssertEqual(best.value as? String, "1")
        best.doubleTap()
        best.typeText(original)
        app.buttons["完成"].tap()
        XCTAssertEqual(best.value as? String, original)

        app.buttons["best-version"].tap()
        XCTAssertTrue(app.buttons["best-version-auto"].waitForExistence(timeout: 5))
        capture(app, name: "best-version-picker")
        app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH %@", "best-version-option-")).firstMatch.tap()
        app.buttons["确定"].tap()
        XCTAssertTrue(app.buttons["best-version-reset"].waitForExistence(timeout: 5))
        app.buttons["best-version-reset"].tap()
        XCTAssertFalse(app.buttons["best-version-reset"].exists)
        capture(app, name: "best-table")

        let row = app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH %@", "best-row-")).firstMatch
        XCTAssertTrue(row.waitForExistence(timeout: 5))
        for _ in 0..<3 where !row.isHittable { app.swipeUp() }
        row.tap()
        XCTAssertTrue(app.buttons["song-detail-back"].waitForExistence(timeout: 5))
        app.buttons["song-detail-back"].tap()
        XCTAssertTrue(row.waitForExistence(timeout: 5))
        app.buttons["best-share"].tap()
        XCTAssertTrue(app.images["chart-poster-preview"].firstMatch.waitForExistence(timeout: 120))
        XCTAssertTrue(app.buttons["poster-share"].exists)
        app.buttons["完成"].tap()
    }

}
