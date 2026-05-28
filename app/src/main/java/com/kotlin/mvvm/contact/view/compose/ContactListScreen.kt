package com.kotlin.mvvm.contact.view.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kotlin.mvvm.contact.model.Contact
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh

@Composable
fun ContactListScreen(
    contacts: List<Contact>,
    isLoading: Boolean,
    snackbarHostState: SnackbarHostState,
    onSyncClick: () -> Unit,
    onBackupClick: () -> Unit,
    onAddClick: () -> Unit,
    onContactClick: (Int) -> Unit
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ContactListHeader(
                onSyncClick = onSyncClick,
                onBackupClick = onBackupClick,
                onAddClick = onAddClick
            )
        },
        bottomBar = {
            AdBanner(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(contacts) { index, contact ->
                    ContactRow(contact = contact, onClick = { onContactClick(index) })
                }
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = ContactOrange
                )
            }
        }
    }
}

@Composable
private fun ContactListHeader(
    onSyncClick: () -> Unit,
    onBackupClick: () -> Unit,
    onAddClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .statusBarsPadding()
            .height(50.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Search",
            color = ContactOrange,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.weight(1f))
        HeaderAction(
            icon = Icons.AutoMirrored.Filled.List,
            contentDescription = "Backup",
            onClick = onBackupClick
        )
        HeaderAction(
            icon = Icons.Default.Refresh,
            contentDescription = "Sync",
            onClick = onSyncClick
        )
        HeaderAction(
            icon = Icons.Default.Add,
            contentDescription = "Add",
            onClick = onAddClick
        )
    }
}

@Composable
private fun HeaderAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(44.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = ContactOrange,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun ContactRow(contact: Contact, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(ContactOrange)
            )
            Spacer(modifier = Modifier.width(25.dp))
            Text(
                text = "${contact.firstName} ${contact.lastName}",
                color = Color.Black,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        HorizontalDivider(color = Color(0xFFDDDDDD), thickness = 2.dp)
    }
}
