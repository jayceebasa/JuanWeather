package com.juanweather.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.juanweather.R
import com.juanweather.data.models.User
import com.juanweather.viewmodel.AuthViewModel

@Composable
fun UserManagementScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit
) {
    // READ: Observe all users from Room via LiveData — UI auto-updates on any DB change
    val users by authViewModel.allUsersLiveData.observeAsState(initial = emptyList())

    val userToDelete = remember { mutableStateOf<User?>(null) }
    val userToEdit = remember { mutableStateOf<User?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Background
        AsyncImage(
            model = R.drawable.background,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x99000000))
        )

        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top Bar ──────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, start = 8.dp, end = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = "Registered Users",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // ── Summary badge ────────────────────────────────────────
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .background(Color(0xFF81C784).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${users.size} account${if (users.size != 1) "s" else ""} in database",
                    color = Color(0xFF81C784),
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── READ: User list (auto-updates via LiveData/Flow) ──────
            if (users.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No registered users yet.\nRegister an account to see it here.",
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        fontSize = 15.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(users, key = { it.id }) { user ->
                        UserCard(
                            user = user,
                            onEditClick = { userToEdit.value = user },
                            onDeleteClick = { userToDelete.value = user }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }

        // ── DELETE Confirmation Dialog ────────────────────────────────
        userToDelete.value?.let { user ->
            AlertDialog(
                onDismissRequest = { userToDelete.value = null },
                containerColor = Color(0xFF1E1E1E),
                title = {
                    Text("Delete Account", color = Color.White, fontWeight = FontWeight.SemiBold)
                },
                text = {
                    Text(
                        "Are you sure you want to delete ${user.name}'s account? This cannot be undone.",
                        color = Color.White.copy(alpha = 0.8f)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            // DELETE: removes the record from Room DB
                            authViewModel.deleteUser(user)
                            userToDelete.value = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350))
                    ) {
                        Text("Delete", color = Color.White)
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { userToDelete.value = null },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                    ) {
                        Text("Cancel", color = Color.White)
                    }
                }
            )
        }

        // ── UPDATE / Edit Dialog ──────────────────────────────────────
        userToEdit.value?.let { user ->
            EditUserDialog(
                user = user,
                onDismiss = { userToEdit.value = null },
                onSave = { updatedUser ->
                    // UPDATE: saves the modified record back to Room DB
                    authViewModel.updateUser(updatedUser)
                    userToEdit.value = null
                }
            )
        }
    }
}

// ── User Card ─────────────────────────────────────────────────────────────────
@Composable
fun UserCard(
    user: User,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E).copy(alpha = 0.9f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar icon
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(Color(0xFF81C784).copy(alpha = 0.2f), RoundedCornerShape(23.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = Color(0xFF81C784),
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // User info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = user.email,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // Role badge
                val isAdminUser = user.role == "admin"
                Box(
                    modifier = Modifier
                        .padding(top = 3.dp)
                        .background(
                            color = if (isAdminUser) Color(0xFFFFB300).copy(alpha = 0.2f)
                                    else Color(0xFF81C784).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = user.role.uppercase(),
                        color = if (isAdminUser) Color(0xFFFFB300) else Color(0xFF81C784),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // EDIT button
            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Edit user",
                    tint = Color(0xFF81C784),
                    modifier = Modifier.size(20.dp)
                )
            }

            // DELETE button
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete user",
                    tint = Color(0xFFEF5350),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ── Edit User Dialog ──────────────────────────────────────────────────────────
@Composable
fun EditUserDialog(
    user: User,
    onDismiss: () -> Unit,
    onSave: (User) -> Unit
) {
    val editedName = remember { mutableStateOf(user.name) }
    val editedEmail = remember { mutableStateOf(user.email) }
    val nameError = remember { mutableStateOf(false) }
    val emailError = remember { mutableStateOf(false) }

    fun isValidEmail(email: String): Boolean {
        val pattern = "^[A-Za-z0-9+_.-]+@(.+)$"
        return email.matches(pattern.toRegex()) && email.contains(".") && email.contains("@")
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E1E1E), RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = "Edit Account",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Name field
                TextField(
                    value = editedName.value,
                    onValueChange = {
                        editedName.value = it
                        nameError.value = false
                    },
                    label = { Text("Full Name", color = Color.White.copy(alpha = 0.7f)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = nameError.value,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.1f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color.White.copy(alpha = 0.7f),
                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                        focusedIndicatorColor = Color(0xFF81C784),
                        unfocusedIndicatorColor = Color.White.copy(alpha = 0.3f)
                    )
                )
                if (nameError.value) {
                    Text("Name cannot be empty", color = Color(0xFFEF5350), fontSize = 11.sp,
                        modifier = Modifier.padding(top = 3.dp))
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Email field
                TextField(
                    value = editedEmail.value,
                    onValueChange = {
                        editedEmail.value = it
                        emailError.value = false
                    },
                    label = { Text("Email", color = Color.White.copy(alpha = 0.7f)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = emailError.value,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.1f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color.White.copy(alpha = 0.7f),
                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                        focusedIndicatorColor = Color(0xFF81C784),
                        unfocusedIndicatorColor = Color.White.copy(alpha = 0.3f)
                    )
                )
                if (emailError.value) {
                    Text("Enter a valid email", color = Color(0xFFEF5350), fontSize = 11.sp,
                        modifier = Modifier.padding(top = 3.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Cancel
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel", color = Color.White)
                    }

                    // Save — UPDATE operation
                    Button(
                        onClick = {
                            nameError.value = editedName.value.isBlank()
                            emailError.value = !isValidEmail(editedEmail.value)
                            if (!nameError.value && !emailError.value) {
                                onSave(user.copy(name = editedName.value.trim(), email = editedEmail.value.trim()))
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF81C784)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save", color = Color.Black, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
