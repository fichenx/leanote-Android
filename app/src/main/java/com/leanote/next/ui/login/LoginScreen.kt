package com.leanote.next.ui.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.leanote.next.ui.components.AccountAvatar
import com.leanote.next.ui.components.LeanoteMark
import com.leanote.next.ui.theme.LocalUiSpacing
import com.leanote.next.ui.theme.LocalCardCorner
import com.leanote.next.util.TimeUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val spacing = LocalUiSpacing.current

    LaunchedEffect(Unit) {
        viewModel.loggedIn.collect { onLoggedIn() }
    }

    val dark = MaterialTheme.colorScheme.background.luminance() < 0.4f
    val heroColor = if (dark) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.primary
    }
    val heroAccent = MaterialTheme.colorScheme.tertiary
    val onHero = if (dark) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onPrimary
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 顶部品牌渐变
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(560.dp)
                .background(
                    Brush.verticalGradient(
                        0f to heroColor,
                        0.55f to heroColor,
                        1f to MaterialTheme.colorScheme.background,
                    )
                )
        )

        // 装饰性光晕
        val transition = rememberInfiniteTransition(label = "glow")
        val pulse by transition.animateFloat(
            initialValue = 0.85f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(5200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "pulse",
        )
        Box(
            modifier = Modifier
                .padding(start = 220.dp, top = 40.dp)
                .size((220 * pulse).dp)
                .clip(CircleShape)
                .background(heroAccent.copy(alpha = 0.12f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = spacing.pageHorizontal),
        ) {
            Spacer(Modifier.height(spacing.pageVertical))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LeanoteMark(modifier = Modifier.size(48.dp), tint = Color.White.copy(alpha = 0.16f))
                Spacer(Modifier.width(spacing.inlineGap))
                Column {
                    Text(
                        "Leanote",
                        color = onHero,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                    )
                    Text(
                        "把灵感留在纸上",
                        color = onHero.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Spacer(Modifier.height(spacing.sectionGap))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(LocalCardCorner.current.dp),
            ) {
                Column(
                    Modifier.padding(
                        horizontal = spacing.cardHorizontal,
                        vertical = spacing.cardVertical,
                    ),
                ) {
                    Text(
                        if (state.register) "创建账户" else "欢迎回来",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (state.register) "在 Leanote 服务器上注册一个新账户" else "登录你的 Leanote，继续记录",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(spacing.contentVertical))

                    OutlinedTextField(
                        value = state.email,
                        onValueChange = viewModel::setEmail,
                        label = { Text("邮箱") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                        ),
                    )
                    Spacer(Modifier.height(spacing.itemGap))
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = viewModel::setPassword,
                        label = { Text("密码") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            viewModel.submit()
                        }),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = null,
                                )
                            }
                        },
                    )

                    AnimatedVisibility(
                        visible = state.showServerField,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        Column {
                            Spacer(Modifier.height(spacing.itemGap))
                            OutlinedTextField(
                                value = state.host,
                                onValueChange = viewModel::setHost,
                                label = { Text("服务器地址") },
                                placeholder = { Text("https://leanote.com") },
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Rounded.Dns, contentDescription = null) },
                                supportingText = { Text("支持自建 Leanote 服务，例如 https://note.example.com") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                            )
                        }
                    }

                    if (state.error != null) {
                        Spacer(Modifier.height(spacing.itemGap))
                        Text(
                            state.error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    Spacer(Modifier.height(spacing.contentVertical))
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.submit()
                        },
                        enabled = !state.loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        if (state.loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            Spacer(Modifier.width(spacing.inlineGap))
                            Text("正在连接…")
                        } else {
                            Text(if (state.register) "注册并登录" else "登录")
                            Spacer(Modifier.width(spacing.inlineGap))
                            Icon(Icons.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(Modifier.height(spacing.itemGap))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(spacing.itemGap),
                    ) {
                        TextButton(onClick = viewModel::toggleMode) {
                            Text(if (state.register) "已有账户？登录" else "注册新账户")
                        }
                        TextButton(onClick = viewModel::toggleServerField) {
                            Icon(Icons.Rounded.Dns, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(spacing.inlineGap))
                            Text(if (state.showServerField) "隐藏服务器" else "服务器地址")
                        }
                    }
                }
            }

            if (state.accounts.isNotEmpty()) {
                Spacer(Modifier.height(spacing.sectionGap))
                Text(
                    "已保存的账户",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = spacing.inlineGap),
                )
                Spacer(Modifier.height(spacing.itemGap))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    shape = RoundedCornerShape(LocalCardCorner.current.dp),
                ) {
                    Column {
                        state.accounts.forEachIndexed { index, account ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.switchAccount(account.id) }
                                    .heightIn(min = 48.dp)
                                    .padding(
                                        horizontal = spacing.cardHorizontal,
                                        vertical = spacing.itemGap,
                                    ),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AccountAvatar(account)
                                Spacer(Modifier.width(spacing.inlineGap))
                                Column(Modifier.weight(1f)) {
                                    Text(account.displayName, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        account.host.removePrefix("https://").removePrefix("http://"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (account.lastSyncAt > 0) {
                                    Text(
                                        TimeUtils.formatList(account.lastSyncAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            if (index < state.accounts.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(
                                        start = spacing.cardHorizontal + 40.dp + spacing.inlineGap,
                                    ),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(spacing.sectionGap))
            Text(
                "Leanote 开源笔记 · 支持 leanote.com 与自建服务器",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(spacing.pageVertical))
        }
    }
}
