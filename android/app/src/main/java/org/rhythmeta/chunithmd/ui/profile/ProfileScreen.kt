package org.rhythmeta.chunithmd.ui.profile

import org.rhythmeta.chunithmd.shared.localization.tr

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.rhythmeta.chunithmd.profile.ProfileAvatarStore
import org.rhythmeta.chunithmd.profile.ProfileRepository
import org.rhythmeta.chunithmd.shared.ProfileDraft
import org.rhythmeta.chunithmd.shared.ProfileServer
import org.rhythmeta.chunithmd.shared.UserProfile
import org.rhythmeta.chunithmd.shared.CatalogVersionFormatter
import org.rhythmeta.chunithmd.ui.components.ExpandableBottomSheet
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference
import org.rhythmeta.chunithmd.ui.components.SquircleExtension
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ProfileScreen(
    modifier: Modifier,
    repository: ProfileRepository,
    avatarStore: ProfileAvatarStore,
    currentVersionByServer: Map<ProfileServer, String?> = emptyMap(),
    createRequested: Boolean,
    onCreateRequestHandled: () -> Unit,
) {
    val profiles by repository.profiles.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var editorProfile by remember { mutableStateOf<UserProfile?>(null) }
    var editorVisible by remember { mutableStateOf(false) }
    var deleteProfile by remember { mutableStateOf<UserProfile?>(null) }
    LaunchedEffect(createRequested) {
        if (createRequested) {
            editorProfile = null
            editorVisible = true
            onCreateRequestHandled()
        }
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (profiles.isEmpty()) {
            item {
                Column(Modifier.fillMaxWidth().padding(top = 100.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Person, null, Modifier.size(52.dp), tint = MiuixTheme.colorScheme.onSurfaceVariantActions)
                    Text(tr("还没有用户档案"), style = MiuixTheme.textStyles.title3, modifier = Modifier.padding(top = 14.dp))
                    Text(tr("创建档案以保存你的本地设置"), color = MiuixTheme.colorScheme.onSurfaceVariantSummary, modifier = Modifier.padding(top = 6.dp))
                    Button(onClick = { editorProfile = null; editorVisible = true }, modifier = Modifier.padding(top = 18.dp), colors = ButtonDefaults.buttonColorsPrimary()) {
                        Icon(Icons.Rounded.PersonAdd, null)
                        Text(tr("新建档案"), Modifier.padding(start = 8.dp))
                    }
                }
            }
        } else {
            items(profiles, key = { it.id }) { profile ->
                ProfileCard(
                    profile = profile,
                    currentVersion = currentVersionByServer[profile.server],
                    onClick = { if (!profile.isActive) scope.launch { repository.activate(profile) } },
                    onEdit = { editorProfile = profile; editorVisible = true },
                    onDelete = { deleteProfile = profile },
                )
            }
        }
    }
    ProfileEditorSheet(
        visible = editorVisible,
        profile = editorProfile,
        repository = repository,
        avatarStore = avatarStore,
        onDismiss = { editorVisible = false },
    )
    deleteProfile?.let { target ->
        Dialog(onDismissRequest = { deleteProfile = null }, properties = DialogProperties(usePlatformDefaultWidth = true)) {
            Card(
                modifier = Modifier.padding(24.dp),
                colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
                insideMargin = PaddingValues(20.dp),
            ) {
                Icon(Icons.Rounded.Warning, null, tint = MiuixTheme.colorScheme.error)
                Text(tr("删除档案？"), style = MiuixTheme.textStyles.title3, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
                Text(tr("删除后本地档案信息将无法恢复。"), color = MiuixTheme.colorScheme.onSurfaceVariantSummary, modifier = Modifier.padding(top = 6.dp))
                Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(text = tr("取消"), onClick = { deleteProfile = null }, modifier = Modifier.weight(1f))
                    Button(onClick = { scope.launch { repository.delete(target); avatarStore.deleteStored(target.avatarPath); deleteProfile = null } }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(color = MiuixTheme.colorScheme.errorContainer, contentColor = MiuixTheme.colorScheme.onErrorContainer)) { Text(tr("删除")) }
                }
            }
        }
    }
}

@Composable
private fun ProfileCard(
    profile: UserProfile,
    currentVersion: String?,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        showIndication = true,
        cornerRadius = 16.dp,
        insideMargin = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        colors = CardDefaults.defaultColors(color = if (profile.isActive) MiuixTheme.colorScheme.secondaryContainer else MiuixTheme.colorScheme.surfaceContainer),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarImage(profile.avatarPath, 52.dp)
            Column(Modifier.weight(1f).padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(profile.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, false))
                    if (profile.isActive) Icon(Icons.Rounded.CheckCircle, tr("当前档案"), Modifier.padding(start = 6.dp).size(17.dp), tint = Color(0xFF35A854))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ServerBadge(profile.server, modifier = Modifier.padding(end = 8.dp))
                    currentVersion?.takeIf(String::isNotBlank)?.let {
                        Text(CatalogVersionFormatter.badge(it), style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }
                }
            }
            IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, tr("编辑")) }
            if (!profile.isActive) IconButton(onClick = onDelete) { Icon(Icons.Rounded.DeleteOutline, tr("删除")) }
        }
    }
}

@Composable
internal fun ProfileEditorSheet(
    visible: Boolean,
    profile: UserProfile?,
    repository: ProfileRepository,
    avatarStore: ProfileAvatarStore,
    onDismiss: () -> Unit,
) {
    // TODO: import scores and calculate Rating/Best/title metadata per profile.
    // TODO: isolate imported score data by profile once the score tables exist.
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val editing = profile != null
    var name by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var server by remember { mutableStateOf(ProfileServer.Jp) }
    var avatarPath by remember { mutableStateOf<String?>(null) }
    var stagedPath by remember { mutableStateOf<String?>(null) }
    var clearAvatar by remember { mutableStateOf(false) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            bitmap = withContext(Dispatchers.IO) {
                runCatching {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                        val maxDimension = maxOf(info.size.width, info.size.height)
                        if (maxDimension > 4096) decoder.setTargetSize(info.size.width * 4096 / maxDimension, info.size.height * 4096 / maxDimension)
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    }
                }.getOrNull()
            }
        }
    }
    LaunchedEffect(visible, profile?.id) {
        if (!visible) return@LaunchedEffect
        name = profile?.name ?: tr("我的档案")
        title = profile?.title.orEmpty()
        server = profile?.server ?: ProfileServer.Jp
        avatarPath = profile?.avatarPath
        stagedPath = null
        clearAvatar = false
        bitmap = null
    }
    fun dismiss() {
        avatarStore.discard(stagedPath)
        stagedPath = null
        onDismiss()
    }
    fun save() {
        scope.launch {
            val draft = ProfileDraft(name = name, server = server, title = title)
            val target = profile ?: repository.create(draft)
            val committed = stagedPath?.let { avatarStore.commit(it, target.id) }
            val updated = if (profile == null) target else repository.update(target, draft)
            val finalPath = when { clearAvatar -> null; committed != null -> committed; else -> avatarPath }
            repository.saveAvatar(updated, finalPath)
            if (clearAvatar || committed != null) avatarStore.deleteStored(target.avatarPath)
            stagedPath = null
            onDismiss()
        }
    }
    ExpandableBottomSheet(
        visible = visible,
        onDismissRequest = ::dismiss,
        expandActionLabel = tr("展开"),
        collapseActionLabel = tr("收起"),
        expandedStateDescription = tr("已展开"),
        halfExpandedStateDescription = tr("半屏"),
        header = {
            IconButton(onClick = ::dismiss, modifier = Modifier.align(Alignment.CenterStart)) { Icon(Icons.Rounded.Close, tr("取消")) }
            Text(if (editing) tr("编辑档案") else tr("新建档案"), style = MiuixTheme.textStyles.title3, modifier = Modifier.align(Alignment.Center))
            IconButton(onClick = ::save, modifier = Modifier.align(Alignment.CenterEnd), enabled = name.trim().isNotEmpty()) { Icon(Icons.Rounded.Check, tr("保存"), tint = MiuixTheme.colorScheme.primary) }
        },
    ) { topInset ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, topInset + 12.dp, 16.dp, 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    AvatarImage(if (clearAvatar) null else (stagedPath ?: avatarPath), 88.dp)
                    Column(Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { photoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Rounded.AddPhotoAlternate, null)
                            Text(tr("选择图片"), Modifier.padding(start = 6.dp))
                        }
                        if (avatarPath != null || stagedPath != null) {
                            Button(
                                onClick = { avatarStore.discard(stagedPath); stagedPath = null; avatarPath = null; clearAvatar = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(),
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, null)
                                Text(tr("清除头像"), Modifier.padding(start = 6.dp))
                            }
                        }
                    }
                }
            }
            item {
                SmallTitle(tr("基本信息"), insideMargin = PaddingValues(horizontal = 0.dp, vertical = 7.dp))
                Card(Modifier.fillMaxWidth(), insideMargin = PaddingValues(14.dp), cornerRadius = 16.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextField(value = name, onValueChange = { name = it }, label = tr("姓名"), useLabelAsPlaceholder = true, singleLine = true, modifier = Modifier.fillMaxWidth())
                        TextField(value = title, onValueChange = { title = it }, label = tr("称号"), useLabelAsPlaceholder = true, singleLine = true, modifier = Modifier.fillMaxWidth())
                        WindowDropdownPreference(items = listOf(tr("日服"), tr("国际服"), tr("国服")), selectedIndex = ProfileServer.entries.indexOf(server), title = tr("服务器"), onSelectedIndexChange = { server = ProfileServer.entries[it] })
                    }
                }
            }
        }
    }
    bitmap?.let { selected ->
        AvatarCropEditor(selected, onDismiss = { bitmap = null }, onApply = { cropped ->
            scope.launch { avatarStore.stage(cropped)?.let { staged -> avatarStore.discard(stagedPath); stagedPath = staged; avatarPath = staged; clearAvatar = false }; bitmap = null }
        })
    }
}

@Composable
internal fun CurrentProfileCard(profile: UserProfile?, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(16.dp), onClick = onClick, showIndication = true, cornerRadius = 18.dp, insideMargin = PaddingValues(14.dp), colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarImage(profile?.avatarPath, 58.dp)
            Column(Modifier.padding(start = 14.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(profile?.name ?: tr("我的档案"), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    profile?.let { ServerBadge(it.server) }
                }
                Text(
                    profile?.title?.takeIf(String::isNotBlank) ?: tr("点击编辑"),
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.Rounded.ChevronRight, tr("打开档案"), tint = MiuixTheme.colorScheme.onSurfaceVariantActions)
        }
    }
}

@Composable
private fun AvatarImage(path: String?, size: androidx.compose.ui.unit.Dp) {
    Box(Modifier.size(size).clip(CircleShape), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.Person, null, Modifier.size(size * .55f), tint = MiuixTheme.colorScheme.onSurfaceVariantActions)
        path?.let { if (File(it).isFile) AsyncImage(File(it), null, Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop) }
    }
}

@Composable
private fun ServerBadge(server: ProfileServer, modifier: Modifier = Modifier) {
    val color = serverColor(server)
    Text(
        text = serverLabel(server),
        style = MiuixTheme.textStyles.footnote2,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = modifier
            .squircleSurface(
                color = color.copy(alpha = 0.14f),
                cornerRadius = 10.dp,
                extension = SquircleExtension,
            )
            .padding(horizontal = 7.dp, vertical = 3.dp),
    )
}

private fun serverLabel(server: ProfileServer) = when (server) { ProfileServer.Jp -> tr("日本"); ProfileServer.Intl -> tr("国际"); ProfileServer.Cn -> tr("中国") }
private fun serverColor(server: ProfileServer) = when (server) { ProfileServer.Jp -> Color(0xFFD9535B); ProfileServer.Intl -> Color(0xFF4B84D9); ProfileServer.Cn -> Color(0xFFE98535) }
