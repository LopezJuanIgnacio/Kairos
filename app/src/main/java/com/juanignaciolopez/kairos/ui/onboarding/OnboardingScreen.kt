package com.juanignaciolopez.kairos.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.juanignaciolopez.kairos.R
import com.juanignaciolopez.kairos.core.components.CircleIcon
import kotlinx.coroutines.launch

private const val LAST_PAGE_INDEX = 3

@Composable
fun OnboardingScreen(
    isHelpMode: Boolean,
    onFinish: () -> Unit,
    onSkip: () -> Unit = onFinish
) {
    val pagerState = rememberPagerState(pageCount = { LAST_PAGE_INDEX + 1 })
    val coroutineScope = rememberCoroutineScope()
    val amber = MaterialTheme.colorScheme.primary
    val background = MaterialTheme.colorScheme.background

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 24.dp)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                when (page) {
                    0 -> IntroPage()
                    1 -> CategoriesPageA()
                    2 -> CategoriesPageB()
                    else -> FeaturesPage()
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val currentPage = pagerState.currentPage
                Text(
                    text = if (currentPage == 0) "Skip" else "Back",
                    color = amber,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(6.dp)
                        .clickable {
                            if (currentPage == 0) {
                                onSkip()
                            } else {
                                coroutineScope.launch { pagerState.animateScrollToPage(currentPage - 1) }
                            }
                        }
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    repeat(LAST_PAGE_INDEX + 1) { index ->
                        val selected = index <= currentPage
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .background(
                                    color = if (selected) amber else background,
                                    shape = CircleShape
                                )
                                .border(
                                    width = 2.dp,
                                    color = amber,
                                    shape = CircleShape
                                )
                        )
                    }
                }

                Text(
                    text = if (currentPage == LAST_PAGE_INDEX) "Finish" else "Next",
                    color = amber,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(6.dp)
                        .clickable {
                            if (currentPage == LAST_PAGE_INDEX) {
                                onFinish()
                            } else {
                                coroutineScope.launch { pagerState.animateScrollToPage(currentPage + 1) }
                            }
                        }
                )
            }
        }
    }
}

@Composable
private fun IntroPage() {
    val amber = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = R.drawable.hourglass_half_solid_full),
            contentDescription = null,
            tint = amber,
            modifier = Modifier.size(92.dp)
        )
        Text(
            text = "Kairos",
            color = amber,
            fontSize = 48.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 52.sp
        )

        Spacer(modifier = Modifier.height(160.dp))

        Text(
            text = "The perfect\nGTD System",
            color = amber,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 36.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CategoriesPageA() {
    val amber = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 24.dp)
    ) {
        Text(
            text = "Categories",
            color = amber,
            fontSize = 54.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(24.dp))
        CategoryItem(
            title = "Recurrent",
            description = "Daily tasks. Every day you'll be reminded of them by a notification."
        )
        Spacer(modifier = Modifier.height(52.dp))
        CategoryItem(
            title = "Actionable",
            description = "Tasks that you could do right now and take under 5 minutes."
        )
        Spacer(modifier = Modifier.height(52.dp))
        CategoryItem(
            title = "Short Term",
            description = "Tasks with a deadline in less than a week. You'll be reminded of them a day earlier."
        )
    }
}

@Composable
private fun CategoriesPageB() {
    val amber = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 24.dp)
    ) {
        Text(
            text = "Categories",
            color = amber,
            fontSize = 54.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(48.dp))
        CategoryItem(
            title = "Long Term",
            description = "Tasks with a deadline in more than a week. You'll be reminded of them a week earlier."
        )
        Spacer(modifier = Modifier.height(68.dp))
        CategoryItem(
            title = "Incubator",
            description = "Tasks that you can't proceed with and therefore don't have a deadline."
        )
    }
}

@Composable
private fun FeaturesPage() {
    val amber = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Create new\ntasks with with\nthe plus button",
            color = amber,
            fontSize = 22.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(26.dp))

        CircleIcon(
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    tint = onPrimary,
                    modifier = Modifier.size(64.dp)
                )
            }
        )

        Spacer(modifier = Modifier.height(44.dp))

        Text(
            text = "Export your\ntasks as events\nfor your\ncalendar app",
            color = amber,
            fontSize = 22.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(26.dp))

        CircleIcon(
            icon = {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = onPrimary,
                    modifier = Modifier.size(52.dp)
                )
            }
        )
    }
}

@Composable
private fun CategoryItem(
    title: String,
    description: String
) {
    val amber = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary

    Column {
        Box(
            modifier = Modifier
                .background(amber, RoundedCornerShape(999.dp))
                .padding(horizontal = 26.dp, vertical = 10.dp)
        ) {
            Text(
                text = title,
                color = onPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Text(
            text = description,
            color = amber,
            fontSize = 17.sp,
            lineHeight = 22.sp,
            modifier = Modifier.padding(top = 18.dp, start = 12.dp, end = 12.dp)
        )
    }
}