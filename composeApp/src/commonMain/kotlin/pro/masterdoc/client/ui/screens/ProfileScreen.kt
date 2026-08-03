package pro.masterdoc.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pro.masterdoc.client.designsystem.components.AppButton
import pro.masterdoc.client.designsystem.components.AppScaffold
import pro.masterdoc.client.designsystem.components.AppText
import pro.masterdoc.client.designsystem.components.AppTextStyle
import pro.masterdoc.client.designsystem.theme.ClientSpacing
import pro.masterdoc.client.navigation.FeatureId
import pro.masterdoc.client.navigation.titleRu
import pro.masterdoc.client.session.SessionUser

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    user: SessionUser? = null,
    features: Set<FeatureId> = emptySet(),
) {
    AppScaffold(title = "Профиль", modifier = modifier) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = ClientSpacing.md, vertical = 24.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ProfileFields(user = user)
            FeatureList(features = features)
            AppButton(
                text = "Выйти",
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ProfileFields(user: SessionUser?) {
    if (user == null) return
    val email = user.email
    val givenName = user.givenName
    val familyName = user.familyName
    val orgName = user.orgName
    if (email == null && givenName == null && familyName == null && orgName == null) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (email != null) {
            ProfileRow(label = "Email", value = email)
        }
        if (givenName != null) {
            ProfileRow(label = "Имя", value = givenName)
        }
        if (familyName != null) {
            ProfileRow(label = "Фамилия", value = familyName)
        }
        if (orgName != null) {
            ProfileRow(label = "Организация", value = orgName)
        }
    }
}

@Composable
private fun FeatureList(features: Set<FeatureId>) {
    val ordered =
        features
            .filter { it != FeatureId.Profile }
            .sortedBy { it.ordinal }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AppText(text = "Доступные фичи", style = AppTextStyle.Title)
        if (ordered.isEmpty()) {
            AppText(text = "Нет дополнительных фич", style = AppTextStyle.Label)
        } else {
            ordered.forEach { feature ->
                AppText(text = feature.titleRu())
            }
        }
    }
}

@Composable
private fun ProfileRow(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        AppText(text = label, style = AppTextStyle.Label)
        AppText(text = value)
    }
}
