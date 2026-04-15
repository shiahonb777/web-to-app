package com.webtoapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.webtoapp.core.auth.AuthResult
import com.webtoapp.core.cloud.*
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.ui.components.EnhancedElevatedCard
import com.webtoapp.ui.components.ThemedBackgroundBox
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * teammanagement
 *
 * createteam, team, , ,
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamScreen(onBack: (() -> Unit)? = null) {
    val apiClient: CloudApiClient = koinInject()
    val scope = rememberCoroutineScope()

    // ── My teams state ──
    var teams by remember { mutableStateOf<List<TeamItem>>(emptyList()) }
    var quotaUsed by remember { mutableIntStateOf(0) }
    var quotaLimit by remember { mutableIntStateOf(0) }
    var memberLimit by remember { mutableIntStateOf(0) }
    var tier by remember { mutableStateOf("free") }
    var isLoading by remember { mutableStateOf(true) }

    // ── Search/Discover state ──
    var selectedTab by remember { mutableIntStateOf(0) } // 0=My Teams, 1=Discover
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<TeamSearchItem>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }

    // ── Expanded team detail state ──
    var expandedTeamId by remember { mutableStateOf<Int?>(null) }
    var expandedSection by remember { mutableStateOf("members") } // members, ranking, requests
    var teamMembers by remember { mutableStateOf<Map<Int, List<TeamMemberItem>>>(emptyMap()) }
    var teamRankings by remember { mutableStateOf<Map<Int, List<TeamRankingItem>>>(emptyMap()) }
    var teamJoinRequests by remember { mutableStateOf<Map<Int, List<TeamJoinRequestItem>>>(emptyMap()) }

    // ── Dialogs ──
    var showCreateDialog by remember { mutableStateOf(false) }
    var showInviteFor by remember { mutableStateOf<Int?>(null) }
    var showJoinDialog by remember { mutableStateOf<TeamSearchItem?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    fun loadTeams() {
        scope.launch {
            isLoading = true
            when (val result = apiClient.listTeams()) {
                is AuthResult.Success -> {
                    teams = result.data.teams
                    quotaUsed = result.data.quotaUsed
                    quotaLimit = result.data.quotaLimit
                    memberLimit = result.data.memberLimit
                    tier = result.data.tier
                }
                else -> {}
            }
            isLoading = false
        }
    }

    fun loadMembers(teamId: Int) {
        scope.launch {
            when (val result = apiClient.getTeamMembers(teamId)) {
                is AuthResult.Success -> {
                    teamMembers = teamMembers + (teamId to result.data)
                }
                else -> {}
            }
        }
    }

    fun loadRanking(teamId: Int) {
        scope.launch {
            when (val result = apiClient.getTeamRanking(teamId)) {
                is AuthResult.Success -> {
                    teamRankings = teamRankings + (teamId to result.data)
                }
                else -> {}
            }
        }
    }

    fun loadJoinRequests(teamId: Int) {
        scope.launch {
            when (val result = apiClient.getJoinRequests(teamId)) {
                is AuthResult.Success -> {
                    teamJoinRequests = teamJoinRequests + (teamId to result.data)
                }
                else -> {}
            }
        }
    }

    fun doSearch() {
        scope.launch {
            isSearching = true
            hasSearched = true
            when (val result = apiClient.searchTeams(searchQuery)) {
                is AuthResult.Success -> searchResults = result.data.teams
                else -> searchResults = emptyList()
            }
            isSearching = false
        }
    }

    LaunchedEffect(Unit) { loadTeams() }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(AppStringsProvider.current().teamTitle, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    onBack?.let {
                        IconButton(onClick = it) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                    }
                },
                actions = {
                    if (quotaUsed < quotaLimit) {
                        IconButton(onClick = { showCreateDialog = true }) {
                            Icon(Icons.Outlined.GroupAdd, null)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        ThemedBackgroundBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ── Tab Row: My Teams | Discover ──
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(AppStringsProvider.current().teamMyTeams, fontSize = 14.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1; if (!hasSearched) doSearch() },
                        text = { Text(AppStringsProvider.current().teamDiscover, fontSize = 14.sp) }
                    )
                }

                when (selectedTab) {
                    0 -> MyTeamsTab(
                        teams = teams,
                        quotaUsed = quotaUsed,
                        quotaLimit = quotaLimit,
                        memberLimit = memberLimit,
                        tier = tier,
                        isLoading = isLoading,
                        expandedTeamId = expandedTeamId,
                        expandedSection = expandedSection,
                        teamMembers = teamMembers,
                        teamRankings = teamRankings,
                        teamJoinRequests = teamJoinRequests,
                        onToggleTeam = { teamId ->
                            if (expandedTeamId == teamId) {
                                expandedTeamId = null
                            } else {
                                expandedTeamId = teamId
                                expandedSection = "members"
                                loadMembers(teamId)
                            }
                        },
                        onSectionChange = { section ->
                            expandedSection = section
                            expandedTeamId?.let { teamId ->
                                when (section) {
                                    "members" -> loadMembers(teamId)
                                    "ranking" -> loadRanking(teamId)
                                    "requests" -> loadJoinRequests(teamId)
                                }
                            }
                        },
                        onInvite = { showInviteFor = it },
                        onRemoveMember = { teamId, memberId ->
                            scope.launch {
                                apiClient.removeTeamMember(teamId, memberId)
                                loadMembers(teamId)
                                loadTeams()
                            }
                        },
                        onDeleteTeam = { teamId ->
                            scope.launch {
                                apiClient.deleteTeam(teamId)
                                loadTeams()
                            }
                        },
                        onReviewRequest = { teamId, requestId, action ->
                            scope.launch {
                                apiClient.reviewJoinRequest(teamId, requestId, action)
                                loadJoinRequests(teamId)
                                loadMembers(teamId)
                                loadTeams()
                            }
                        },
                        onCreateTeam = { showCreateDialog = true }
                    )
                    1 -> DiscoverTab(
                        searchQuery = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onSearch = { doSearch() },
                        searchResults = searchResults,
                        isSearching = isSearching,
                        hasSearched = hasSearched,
                        onJoin = { showJoinDialog = it }
                    )
                }
            }
        }
    }

    // ── Create team dialog ──
    if (showCreateDialog) {
        CreateTeamDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, desc ->
                scope.launch {
                    apiClient.createTeam(name, desc)
                    loadTeams()
                    showCreateDialog = false
                }
            }
        )
    }

    // ── Invite member dialog ──
    showInviteFor?.let { teamId ->
        InviteMemberDialog(
            onDismiss = { showInviteFor = null },
            onInvite = { username, role ->
                scope.launch {
                    apiClient.inviteTeamMember(teamId, username, role)
                    loadMembers(teamId)
                    loadTeams()
                    showInviteFor = null
                }
            }
        )
    }

    // ── Join team dialog ──
    showJoinDialog?.let { team ->
        JoinTeamDialog(
            teamName = team.name,
            onDismiss = { showJoinDialog = null },
            onJoin = { message ->
                scope.launch {
                    when (apiClient.requestJoinTeam(team.id, message)) {
                        is AuthResult.Success -> {
                            snackbarHostState.showSnackbar(AppStringsProvider.current().teamJoinSent)
                            doSearch() // refresh search results
                        }
                        is AuthResult.Error -> {
                            snackbarHostState.showSnackbar("Failed")
                        }
                    }
                    showJoinDialog = null
                }
            }
        )
    }
}


// ═══════════════════════════════════════════
// My Teams Tab
// ═══════════════════════════════════════════

@Composable
private fun MyTeamsTab(
    teams: List<TeamItem>,
    quotaUsed: Int,
    quotaLimit: Int,
    memberLimit: Int,
    tier: String,
    isLoading: Boolean,
    expandedTeamId: Int?,
    expandedSection: String,
    teamMembers: Map<Int, List<TeamMemberItem>>,
    teamRankings: Map<Int, List<TeamRankingItem>>,
    teamJoinRequests: Map<Int, List<TeamJoinRequestItem>>,
    onToggleTeam: (Int) -> Unit,
    onSectionChange: (String) -> Unit,
    onInvite: (Int) -> Unit,
    onRemoveMember: (Int, Int) -> Unit,
    onDeleteTeam: (Int) -> Unit,
    onReviewRequest: (Int, Int, String) -> Unit,
    onCreateTeam: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Quota info ──
        if (quotaLimit > 0) {
            item {
                EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "${AppStringsProvider.current().teamQuota}: $quotaUsed / $quotaLimit",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "$tier · ${AppStringsProvider.current().teamMembers}: $memberLimit max",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (quotaUsed < quotaLimit) {
                            FilledTonalButton(
                                onClick = onCreateTeam,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Outlined.Add, null, Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(AppStringsProvider.current().teamCreate, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        // ── Loading ──
        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        // ── Empty state ──
        if (!isLoading && teams.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        modifier = Modifier.size(80.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Groups,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        AppStringsProvider.current().teamEmpty,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (quotaLimit > 0) {
                        Button(
                            onClick = onCreateTeam,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(AppStringsProvider.current().teamCreate)
                        }
                    }
                }
            }
        }

        // ── Team list ──
        items(teams, key = { it.id }) { team ->
            TeamCard(
                team = team,
                isExpanded = expandedTeamId == team.id,
                expandedSection = expandedSection,
                members = teamMembers[team.id] ?: emptyList(),
                rankings = teamRankings[team.id] ?: emptyList(),
                joinRequests = teamJoinRequests[team.id] ?: emptyList(),
                memberLimit = memberLimit,
                onToggle = { onToggleTeam(team.id) },
                onSectionChange = onSectionChange,
                onInvite = { onInvite(team.id) },
                onRemoveMember = { memberId -> onRemoveMember(team.id, memberId) },
                onDeleteTeam = { onDeleteTeam(team.id) },
                onReviewRequest = { requestId, action -> onReviewRequest(team.id, requestId, action) }
            )
        }
    }
}


// ═══════════════════════════════════════════
// Discover Tab
// ═══════════════════════════════════════════

@Composable
private fun DiscoverTab(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    searchResults: List<TeamSearchItem>,
    isSearching: Boolean,
    hasSearched: Boolean,
    onJoin: (TeamSearchItem) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Search bar ──
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(AppStringsProvider.current().teamSearch) },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange(""); onSearch() }) {
                            Icon(Icons.Filled.Close, null, Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent,
                )
            )
        }

        // Search button
        item {
            FilledTonalButton(
                onClick = onSearch,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Outlined.Search, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(AppStringsProvider.current().teamSearch)
            }
        }

        // ── Loading ──
        if (isSearching) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                }
            }
        }

        // ── Empty state ──
        if (!isSearching && hasSearched && searchResults.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Outlined.SearchOff, null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        AppStringsProvider.current().teamSearchEmpty,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ── Search results ──
        items(searchResults, key = { it.id }) { team ->
            SearchResultCard(team = team, onJoin = { onJoin(team) })
        }
    }
}


// ═══════════════════════════════════════════
// Search Result Card
// ═══════════════════════════════════════════

@Composable
private fun SearchResultCard(
    team: TeamSearchItem,
    onJoin: () -> Unit
) {
    EnhancedElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Team avatar
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
            ) {
                Icon(
                    Icons.Outlined.Groups, null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    team.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${team.memberCount} ${AppStringsProvider.current().teamMembers} · ${team.ownerName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                team.description?.let { desc ->
                    if (desc.isNotBlank()) {
                        Text(
                            desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            // Join button / status
            when {
                team.isMember -> {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            AppStringsProvider.current().teamJoined,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                team.hasPendingRequest -> {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            AppStringsProvider.current().teamJoinPending,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                else -> {
                    FilledTonalButton(
                        onClick = onJoin,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Outlined.PersonAdd, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(AppStringsProvider.current().teamJoin, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}


// ═══════════════════════════════════════════
// Team card with expandable sections
// ═══════════════════════════════════════════

@Composable
private fun TeamCard(
    team: TeamItem,
    isExpanded: Boolean,
    expandedSection: String,
    members: List<TeamMemberItem>,
    rankings: List<TeamRankingItem>,
    joinRequests: List<TeamJoinRequestItem>,
    memberLimit: Int,
    onToggle: () -> Unit,
    onSectionChange: (String) -> Unit,
    onInvite: () -> Unit,
    onRemoveMember: (Int) -> Unit,
    onDeleteTeam: () -> Unit,
    onReviewRequest: (Int, String) -> Unit
) {
    EnhancedElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ) {
                    Icon(
                        Icons.Outlined.Groups, null,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            team.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        // Pending requests badge
                        if (team.pendingRequests > 0) {
                            Spacer(Modifier.width(8.dp))
                            Badge(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ) {
                                Text("${team.pendingRequests}")
                            }
                        }
                    }
                    Text(
                        "${team.memberCount} ${AppStringsProvider.current().teamMembers} · ${team.ownerName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Expanded content
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    // Description
                    team.description?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    // Section tabs: Members | Ranking | Requests
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SectionChip(
                            label = AppStringsProvider.current().teamMembers,
                            icon = Icons.Outlined.People,
                            selected = expandedSection == "members",
                            onClick = { onSectionChange("members") }
                        )
                        SectionChip(
                            label = AppStringsProvider.current().teamRanking,
                            icon = Icons.Outlined.EmojiEvents,
                            selected = expandedSection == "ranking",
                            onClick = { onSectionChange("ranking") }
                        )
                        SectionChip(
                            label = AppStringsProvider.current().teamJoinRequests,
                            icon = Icons.Outlined.PersonAdd,
                            selected = expandedSection == "requests",
                            badge = team.pendingRequests,
                            onClick = { onSectionChange("requests") }
                        )
                    }

                    // Section content
                    when (expandedSection) {
                        "members" -> MembersSection(
                            members = members,
                            memberLimit = memberLimit,
                            onInvite = onInvite,
                            onRemoveMember = onRemoveMember
                        )
                        "ranking" -> RankingSection(rankings = rankings)
                        "requests" -> JoinRequestsSection(
                            requests = joinRequests,
                            onReview = onReviewRequest
                        )
                    }

                    // Delete team button
                    OutlinedButton(
                        onClick = onDeleteTeam,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 8.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Outlined.DeleteOutline, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(AppStringsProvider.current().teamDelete, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}


// ═══════════════════════════════════════════
// Section Chip
// ═══════════════════════════════════════════

@Composable
private fun SectionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    badge: Int = 0,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, fontSize = 12.sp)
                if (badge > 0) {
                    Spacer(Modifier.width(4.dp))
                    Badge(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.size(16.dp)
                    ) {
                        Text("$badge", fontSize = 9.sp)
                    }
                }
            }
        },
        leadingIcon = {
            Icon(icon, null, Modifier.size(16.dp))
        }
    )
}


// ═══════════════════════════════════════════
// Members Section
// ═══════════════════════════════════════════

@Composable
private fun MembersSection(
    members: List<TeamMemberItem>,
    memberLimit: Int,
    onInvite: () -> Unit,
    onRemoveMember: (Int) -> Unit
) {
    Column {
        members.forEach { member ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                ) {
                    Icon(
                        Icons.Outlined.Person, null,
                        Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        member.displayName ?: member.username,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "@${member.username}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Role badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = roleColor(member.role).copy(alpha = 0.12f)
                ) {
                    Text(
                        roleLabel(member.role),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = roleColor(member.role)
                    )
                }
                // Remove button (not for owner)
                if (member.role != "owner") {
                    IconButton(
                        onClick = { onRemoveMember(member.id) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Outlined.PersonRemove, null,
                            Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // Invite button
        if (members.size < memberLimit) {
            FilledTonalButton(
                onClick = onInvite,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Outlined.PersonAdd, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(AppStringsProvider.current().teamInvite, fontSize = 13.sp)
            }
        }
    }
}


// ═══════════════════════════════════════════
// Ranking Section
// ═══════════════════════════════════════════

@Composable
private fun RankingSection(rankings: List<TeamRankingItem>) {
    if (rankings.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
        }
        return
    }

    Column {
        rankings.forEachIndexed { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Rank badge
                val rankColor = when (index) {
                    0 -> Color(0xFFF59E0B)  // Gold
                    1 -> Color(0xFF94A3B8)  // Silver
                    2 -> Color(0xFFCD7F32)  // Bronze
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                }
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = rankColor.copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "#${item.rank}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = rankColor
                        )
                    }
                }

                // Name
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.displayName ?: item.username,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "@${item.username}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Role badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = roleColor(item.role).copy(alpha = 0.12f)
                ) {
                    Text(
                        roleLabel(item.role),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = roleColor(item.role)
                    )
                }

                // Contribution score
                Text(
                    "${item.contribution}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}


// ═══════════════════════════════════════════
// Join Requests Section
// ═══════════════════════════════════════════

@Composable
private fun JoinRequestsSection(
    requests: List<TeamJoinRequestItem>,
    onReview: (Int, String) -> Unit
) {
    if (requests.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Outlined.CheckCircleOutline, null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    AppStringsProvider.current().teamJoinNoRequests,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    Column {
        requests.forEach { request ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                ) {
                    Icon(
                        Icons.Outlined.Person, null,
                        Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        request.displayName ?: request.username,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "@${request.username}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    request.message?.let { msg ->
                        if (msg.isNotBlank()) {
                            Surface(
                                modifier = Modifier.padding(top = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Text(
                                    msg,
                                    modifier = Modifier.padding(8.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    request.createdAt?.let {
                        Text(
                            it.take(10),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                // Action buttons
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalButton(
                        onClick = { onReview(request.id, "approve") },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFF22C55E).copy(alpha = 0.15f),
                            contentColor = Color(0xFF16A34A)
                        )
                    ) {
                        Text(AppStringsProvider.current().teamJoinApprove, fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = { onReview(request.id, "reject") },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                    ) {
                        Text(AppStringsProvider.current().teamJoinReject, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}


// ═══════════════════════════════════════════
// Dialogs
// ═══════════════════════════════════════════

@Composable
private fun CreateTeamDialog(onDismiss: () -> Unit, onCreate: (String, String?) -> Unit) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStringsProvider.current().teamCreate) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(AppStringsProvider.current().teamName) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text(AppStringsProvider.current().teamDesc) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name, desc.ifBlank { null }) },
                enabled = name.isNotBlank()
            ) {
                Text(AppStringsProvider.current().teamCreate)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStringsProvider.current().cancel)
            }
        }
    )
}

@Composable
private fun InviteMemberDialog(onDismiss: () -> Unit, onInvite: (String, String) -> Unit) {
    var username by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("viewer") }
    val roles = listOf("viewer", "editor", "admin")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStringsProvider.current().teamInvite) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(AppStringsProvider.current().teamUsername) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text(
                    AppStringsProvider.current().teamRole,
                    style = MaterialTheme.typography.labelMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    roles.forEach { role ->
                        FilterChip(
                            selected = selectedRole == role,
                            onClick = { selectedRole = role },
                            label = { Text(roleLabel(role), fontSize = 12.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onInvite(username.trim(), selectedRole) },
                enabled = username.isNotBlank()
            ) {
                Text(AppStringsProvider.current().teamInvite)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStringsProvider.current().cancel)
            }
        }
    )
}

@Composable
private fun JoinTeamDialog(
    teamName: String,
    onDismiss: () -> Unit,
    onJoin: (String?) -> Unit
) {
    var message by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${AppStringsProvider.current().teamJoin} · $teamName") },
        text = {
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text(AppStringsProvider.current().teamJoinMessage) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )
        },
        confirmButton = {
            TextButton(onClick = { onJoin(message.ifBlank { null }) }) {
                Text(AppStringsProvider.current().teamJoin)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStringsProvider.current().cancel)
            }
        }
    )
}


// ── Helpers ──

private fun roleColor(role: String): Color = when (role) {
    "owner" -> Color(0xFFF59E0B)
    "admin" -> Color(0xFF8B5CF6)
    "editor" -> Color(0xFF3B82F6)
    else -> Color(0xFF6B7280)
}

private fun roleLabel(role: String): String = when (role) {
    "owner" -> AppStringsProvider.current().teamRoleOwner
    "admin" -> AppStringsProvider.current().teamRoleAdmin
    "editor" -> AppStringsProvider.current().teamRoleEditor
    "viewer" -> AppStringsProvider.current().teamRoleViewer
    else -> role
}
