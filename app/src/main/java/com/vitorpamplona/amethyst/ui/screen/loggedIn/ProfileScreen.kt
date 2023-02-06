package com.vitorpamplona.amethyst.ui.screen

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Surface
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.TabRowDefaults
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.pagerTabIndicatorOffset
import com.google.accompanist.pager.rememberPagerState
import com.vitorpamplona.amethyst.LocalPreferences
import com.vitorpamplona.amethyst.R
import com.vitorpamplona.amethyst.model.Account
import com.vitorpamplona.amethyst.model.Note
import com.vitorpamplona.amethyst.model.User
import com.vitorpamplona.amethyst.model.toNote
import com.vitorpamplona.amethyst.service.NostrUserProfileDataSource
import com.vitorpamplona.amethyst.service.NostrUserProfileFollowersDataSource
import com.vitorpamplona.amethyst.service.NostrUserProfileFollowsDataSource
import com.vitorpamplona.amethyst.service.model.ReportEvent
import com.vitorpamplona.amethyst.ui.actions.NewChannelView
import com.vitorpamplona.amethyst.ui.actions.NewRelayListView
import com.vitorpamplona.amethyst.ui.actions.NewUserMetadataView
import com.vitorpamplona.amethyst.ui.note.UserPicture
import com.vitorpamplona.amethyst.ui.screen.loggedIn.AccountViewModel
import kotlinx.coroutines.launch
import nostr.postr.toNpub
import nostr.postr.toNsec

@OptIn(ExperimentalPagerApi::class)
@Composable
fun ProfileScreen(userId: String?, accountViewModel: AccountViewModel, navController: NavController) {
    val accountState by accountViewModel.accountLiveData.observeAsState()
    val account = accountState?.account ?: return

    if (userId == null) return

    DisposableEffect(account) {
        NostrUserProfileDataSource.loadUserProfile(userId)
        NostrUserProfileFollowersDataSource.loadUserProfile(userId)
        NostrUserProfileFollowsDataSource.loadUserProfile(userId)

        onDispose {
            NostrUserProfileDataSource.stop()
            NostrUserProfileFollowsDataSource.stop()
            NostrUserProfileFollowersDataSource.stop()
        }
    }

    val baseUser = NostrUserProfileDataSource.user ?: return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colors.background
    ) {
        Column() {
            ProfileHeader(baseUser, navController, account, accountViewModel)

            val pagerState = rememberPagerState()
            val coroutineScope = rememberCoroutineScope()

            Column(modifier = Modifier.padding()) {
                ScrollableTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        indicator = { tabPositions ->
                            TabRowDefaults.Indicator(
                                Modifier.pagerTabIndicatorOffset(pagerState, tabPositions),
                                color = MaterialTheme.colors.primary
                            )
                        },
                        edgePadding = 8.dp
                ) {
                    Tab(
                        selected = pagerState.currentPage == 0,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                        text = {
                            Text(text = "记录")
                        }
                    )

                    Tab(
                        selected = pagerState.currentPage == 1,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                        text = {
                            val userState by baseUser.liveFollows.observeAsState()
                            val userFollows = userState?.user?.follows?.size ?: "--"

                            Text(text = "$userFollows 关注")
                        }
                    )

                    Tab(
                        selected = pagerState.currentPage == 2,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(2) } },
                        text = {
                            val userState by baseUser.liveFollows.observeAsState()
                            val userFollows = userState?.user?.followers?.size ?: "--"

                            Text(text = "$userFollows 被关注")
                        }
                    )

                    Tab(
                        selected = pagerState.currentPage == 3,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(3) } },
                        text = {
                            val userState by baseUser.liveRelays.observeAsState()
                            val userRelaysBeingUsed = userState?.user?.relaysBeingUsed?.size ?: "--"

                            val userStateRelayInfo by baseUser.liveRelayInfo.observeAsState()
                            val userRelays = userStateRelayInfo?.user?.relays?.size ?: "--"

                            Text(text = "$userRelaysBeingUsed / $userRelays 中继器")
                        }
                    )
                }
                HorizontalPager(count = 4, state = pagerState) {
                    when (pagerState.currentPage) {
                        0 -> TabNotes(baseUser, accountViewModel, navController)
                        1 -> TabFollows(baseUser, accountViewModel, navController)
                        2 -> TabFollowers(baseUser, accountViewModel, navController)
                        3 -> TabRelays(baseUser, accountViewModel, navController)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    baseUser: User,
    navController: NavController,
    account: Account,
    accountViewModel: AccountViewModel
) {
    val ctx = LocalContext.current.applicationContext
    var popupExpanded by remember { mutableStateOf(false) }

    val accountUserState by account.userProfile().liveFollows.observeAsState()
    val accountUser = accountUserState?.user ?: return

    Box {
        DrawBanner(baseUser)

        Box(modifier = Modifier
            .padding(horizontal = 10.dp)
            .size(40.dp)
            .align(Alignment.TopEnd)) {

            Button(
                modifier = Modifier
                    .size(30.dp)
                    .align(Alignment.Center),
                onClick = { popupExpanded = true },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults
                    .buttonColors(
                        backgroundColor = MaterialTheme.colors.background
                    ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    tint = Color.White,
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "更多选项",
                )

                UserProfileDropDownMenu(baseUser, popupExpanded, { popupExpanded = false }, accountViewModel)
            }

        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
                .padding(top = 75.dp)
        ) {

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {

                UserPicture(
                    baseUser, navController, account.userProfile(), 100.dp,
                    pictureModifier = Modifier.border(
                        3.dp,
                        MaterialTheme.colors.background,
                        CircleShape
                    )
                )

                Spacer(Modifier.weight(1f))

                Row(modifier = Modifier
                    .height(35.dp)
                    .padding(bottom = 3.dp)) {
                    MessageButton(baseUser, navController)

                    if (accountUser == baseUser && account.isWriteable()) {
                        NSecCopyButton(account)
                    }

                    NPubCopyButton(baseUser)

                    if (accountUser == baseUser) {
                        EditButton(account)
                    } else {
                        if (account.isHidden(baseUser)) {
                            ShowUserButton {
                                account.showUser(baseUser.pubkeyHex)
                                LocalPreferences(ctx).saveToEncryptedStorage(account)
                            }
                        } else if (accountUser.isFollowing(baseUser)) {
                            UnfollowButton { account.unfollow(baseUser) }
                        } else {
                            FollowButton { account.follow(baseUser) }
                        }
                    }
                }
            }

            DrawAdditionalInfo(baseUser)

            Divider(modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun DrawAdditionalInfo(baseUser: User) {
    val userState by baseUser.liveMetadata.observeAsState()
    val user = userState?.user ?: return

    Text(
        user.bestDisplayName() ?: "",
        modifier = Modifier.padding(top = 7.dp),
        fontWeight = FontWeight.Bold,
        fontSize = 25.sp
    )
    Text(
        " @${user.bestUsername()}",
        color = MaterialTheme.colors.onSurface.copy(alpha = 0.32f)
    )
    Text(
        "${user.info.about}",
        color = MaterialTheme.colors.onSurface,
        modifier = Modifier.padding(top = 5.dp, bottom = 5.dp)
    )
}

@Composable
private fun DrawBanner(baseUser: User) {
    val userState by baseUser.liveMetadata.observeAsState()
    val user = userState?.user ?: return

    val banner = user.info.banner

    if (banner != null && banner.isNotBlank()) {
        AsyncImage(
            model = banner,
            contentDescription = "个人头像",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .height(125.dp)
        )
    } else {
        Image(
            painter = painterResource(R.drawable.profile_banner),
            contentDescription = "背景图",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .height(125.dp)
        )
    }
}

@Composable
fun TabNotes(user: User, accountViewModel: AccountViewModel, navController: NavController) {
    val accountState by accountViewModel.accountLiveData.observeAsState()
    if (accountState != null) {
        val feedViewModel: NostrUserProfileFeedViewModel = viewModel()

        Column(Modifier.fillMaxHeight()) {
            Column(
                modifier = Modifier.padding(vertical = 0.dp)
            ) {
                FeedView(feedViewModel, accountViewModel, navController, null)
            }
        }
    }
}

@Composable
fun TabFollows(user: User, accountViewModel: AccountViewModel, navController: NavController) {
    val feedViewModel: NostrUserProfileFollowsUserFeedViewModel = viewModel()

    Column(Modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier.padding(vertical = 0.dp)
        ) {
            UserFeedView(feedViewModel, accountViewModel, navController)
        }
    }
}

@Composable
fun TabFollowers(user: User, accountViewModel: AccountViewModel, navController: NavController) {
    val feedViewModel: NostrUserProfileFollowersUserFeedViewModel = viewModel()

    Column(Modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier.padding(vertical = 0.dp)
        ) {
            UserFeedView(feedViewModel, accountViewModel, navController)
        }
    }
}

@Composable
fun TabRelays(user: User, accountViewModel: AccountViewModel, navController: NavController) {
    val feedViewModel: RelayFeedViewModel = viewModel()

    LaunchedEffect(key1 = user) {
        feedViewModel.subscribeTo(user)
    }

    Column(Modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier.padding(vertical = 0.dp)
        ) {
            RelayFeedView(feedViewModel, accountViewModel, navController)
        }
    }
}

@Composable
private fun NSecCopyButton(
    account: Account
) {
    val clipboardManager = LocalClipboardManager.current
    var popupExpanded by remember { mutableStateOf(false) }

    Button(
        modifier = Modifier
            .padding(horizontal = 3.dp)
            .width(50.dp),
        onClick = { popupExpanded = true },
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults
            .buttonColors(
                backgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.32f)
            )
    ) {
        Icon(
            tint = Color.White,
            imageVector = Icons.Default.Key,
            contentDescription = "Copies the Nsec ID (your password) to the clipboard for backup"
        )

        DropdownMenu(
            expanded = popupExpanded,
            onDismissRequest = { popupExpanded = false }
        ) {
            DropdownMenuItem(onClick = {  account.loggedIn.privKey?.let { clipboardManager.setText(AnnotatedString(it.toNsec())) }; popupExpanded = false }) {
                Text("复制私钥到粘贴板")
            }
        }
    }
}

@Composable
private fun NPubCopyButton(
    user: User
) {
    val clipboardManager = LocalClipboardManager.current
    var popupExpanded by remember { mutableStateOf(false) }

    Button(
        modifier = Modifier
            .padding(horizontal = 3.dp)
            .width(50.dp),
        onClick = { popupExpanded = true },
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults
            .buttonColors(
                backgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.32f)
            ),
    ) {
        Icon(
            tint = Color.White,
            imageVector = Icons.Default.Share,
            contentDescription = "Copies the public key to the clipboard for sharing"
        )

        DropdownMenu(
            expanded = popupExpanded,
            onDismissRequest = { popupExpanded = false }
        ) {
            DropdownMenuItem(onClick = { clipboardManager.setText(AnnotatedString(user.pubkey.toNpub())); popupExpanded = false }) {
                Text("复制公钥到粘贴板(NPub)")
            }
        }
    }
}

@Composable
private fun MessageButton(user: User, navController: NavController) {
    Button(
        modifier = Modifier
            .padding(horizontal = 3.dp)
            .width(50.dp),
        onClick = { navController.navigate("Room/${user.pubkeyHex}") },
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults
            .buttonColors(
                backgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.32f)
            ),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_dm),
            "Send a Direct Message",
            modifier = Modifier.size(20.dp),
            tint = Color.White
        )
    }
}

@Composable
private fun EditButton(account: Account) {
    var wantsToEdit by remember {
        mutableStateOf(false)
    }

    if (wantsToEdit)
        NewUserMetadataView({ wantsToEdit = false }, account)

    Button(
        modifier = Modifier
            .padding(horizontal = 3.dp)
            .width(50.dp),
        onClick = { wantsToEdit = true },
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults
            .buttonColors(
                backgroundColor = MaterialTheme.colors.primary
            )
    ) {
        Icon(
            tint = Color.White,
            imageVector = Icons.Default.EditNote,
            contentDescription = "编辑用户信息"
        )
    }
}

@Composable
fun UnfollowButton(onClick: () -> Unit) {
    Button(
        modifier = Modifier.padding(horizontal = 3.dp),
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults
            .buttonColors(
                backgroundColor = MaterialTheme.colors.primary
            ),
        contentPadding = PaddingValues(vertical = 6.dp, horizontal = 16.dp)
    ) {
        Text(text = "取消关注", color = Color.White)
    }
}

@Composable
fun FollowButton(onClick: () -> Unit) {
    Button(
        modifier = Modifier.padding(start = 3.dp),
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults
            .buttonColors(
                backgroundColor = MaterialTheme.colors.primary
            ),
        contentPadding = PaddingValues(vertical = 6.dp, horizontal = 16.dp)
    ) {
        Text(text = "Follow", color = Color.White, textAlign = TextAlign.Center)
    }
}

@Composable
fun ShowUserButton(onClick: () -> Unit) {
    Button(
        modifier = Modifier.padding(start = 3.dp),
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults
            .buttonColors(
                backgroundColor = MaterialTheme.colors.primary
            ),
        contentPadding = PaddingValues(vertical = 6.dp, horizontal = 16.dp)
    ) {
        Text(text = "Unblock", color = Color.White)
    }
}


@Composable
fun UserProfileDropDownMenu(user: User, popupExpanded: Boolean, onDismiss: () -> Unit, accountViewModel: AccountViewModel) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current.applicationContext

    val accountState by accountViewModel.accountLiveData.observeAsState()
    val account = accountState?.account ?: return

    DropdownMenu(
        expanded = popupExpanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(onClick = { clipboardManager.setText(AnnotatedString(user.pubkey.toNpub() ?: "")); onDismiss() }) {
            Text("复制用户 ID")
        }

        if ( account.userProfile() != user) {
            Divider()
            if (account.isHidden(user)) {
                DropdownMenuItem(onClick = {
                    user.let {
                        accountViewModel.show(
                            it,
                            context
                        )
                    }; onDismiss()
                }) {
                    Text("取消屏蔽 用户")
                }
            } else {
                DropdownMenuItem(onClick = { user.let { accountViewModel.hide(it, context) }; onDismiss() }) {
                    Text("隐藏用户")
                }
            }
            Divider()
            DropdownMenuItem(onClick = {
                accountViewModel.report(user, ReportEvent.ReportType.SPAM);
                user.let { accountViewModel.hide(it, context) }
                onDismiss()
            }) {
                Text("Report Spam / Scam")
            }
            DropdownMenuItem(onClick = {
                accountViewModel.report(user, ReportEvent.ReportType.IMPERSONATION);
                user.let { accountViewModel.hide(it, context) }
                onDismiss()
            }) {
                Text("Report Impersonation")
            }
            DropdownMenuItem(onClick = {
                accountViewModel.report(user, ReportEvent.ReportType.EXPLICIT);
                user.let { accountViewModel.hide(it, context) }
                onDismiss()
            }) {
                Text("Report Explicit Content")
            }
            DropdownMenuItem(onClick = {
                accountViewModel.report(user, ReportEvent.ReportType.ILLEGAL);
                user.let { accountViewModel.hide(it, context) }
                onDismiss()
            }) {
                Text("Report Illegal Behaviour")
            }
        }
    }
}