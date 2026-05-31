package com.madmaxlgndklr.pokedex.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madmaxlgndklr.pokedex.R
import com.madmaxlgndklr.pokedex.ui.theme.CaughtGold
import com.madmaxlgndklr.pokedex.ui.theme.GlowBlue
import com.madmaxlgndklr.pokedex.ui.theme.PokedexCream
import com.madmaxlgndklr.pokedex.ui.theme.PokedexDark
import com.madmaxlgndklr.pokedex.ui.theme.PressStart2P
import com.madmaxlgndklr.pokedex.data.remote.SupabaseModule
import io.github.jan.supabase.compose.auth.composeAuth
import io.github.jan.supabase.compose.auth.composable.NativeSignInResult
import io.github.jan.supabase.compose.auth.composable.rememberSignInWithGoogle

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit
) {
    val trainerName by viewModel.trainerName.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    var draft by remember { mutableStateOf<String?>(null) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current

    val displayed = draft ?: trainerName
    val isAnonymous = viewModel.isAnonymous

    val googleSignIn = SupabaseModule.client.composeAuth.rememberSignInWithGoogle(
        onResult = { result ->
            when (result) {
                is NativeSignInResult.Success -> viewModel.onGoogleSignInResult()
                is NativeSignInResult.Error -> { }
                else -> {}
            }
        }
    )

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val sw = maxWidth
        val sh = maxHeight

        Image(
            painter = painterResource(R.drawable.pdex_open_v2),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        IconButton(
            onClick = onBack,
            modifier = Modifier.offset(x = 2.dp, y = 2.dp).size(36.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = PokedexCream)
        }

        Column(
            modifier = Modifier
                .offset(x = sw * 0.04f, y = sh * 0.25f)
                .fillMaxWidth(0.92f)
                .height(sh * 0.65f)
                .background(PokedexDark.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("TRAINER PROFILE", fontFamily = PressStart2P, fontSize = 8.sp, color = CaughtGold)
            HorizontalDivider(color = PokedexCream.copy(alpha = 0.25f))

            Text("TRAINER NAME", fontFamily = PressStart2P, fontSize = 6.sp, color = PokedexCream.copy(alpha = 0.6f))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = displayed,
                    onValueChange = { draft = it.take(16) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontFamily = PressStart2P, fontSize = 7.sp, color = PokedexCream),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GlowBlue,
                        unfocusedBorderColor = PokedexCream.copy(alpha = 0.3f)
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        keyboard?.hide()
                        draft?.let { viewModel.saveTrainerName(it); draft = null }
                    })
                )
                TextButton(
                    onClick = { draft?.let { viewModel.saveTrainerName(it); draft = null } },
                    enabled = draft != null
                ) {
                    Text("SAVE", fontFamily = PressStart2P, fontSize = 6.sp, color = if (draft != null) GlowBlue else PokedexCream.copy(alpha = 0.3f))
                }
            }

            HorizontalDivider(color = PokedexCream.copy(alpha = 0.25f))
            Text("SYNC ACCOUNT", fontFamily = PressStart2P, fontSize = 6.sp, color = PokedexCream.copy(alpha = 0.6f))

            if (currentUser != null && !isAnonymous) {
                Text(
                    text = (currentUser!!.email ?: "GOOGLE ACCOUNT").uppercase(),
                    fontFamily = PressStart2P, fontSize = 6.sp, color = CaughtGold,
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(onClick = { viewModel.signOut() }) {
                    Text("SIGN OUT", fontFamily = PressStart2P, fontSize = 6.sp, color = PokedexCream.copy(alpha = 0.6f))
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                    listOf(false to "SIGN IN", true to "CREATE ACCOUNT").forEach { (signup, label) ->
                        TextButton(
                            onClick = { isSignUp = signup; error?.let { viewModel.clearError() } },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                label, fontFamily = PressStart2P, fontSize = 5.sp,
                                color = if (isSignUp == signup) GlowBlue else PokedexCream.copy(alpha = 0.4f)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    label = { Text("EMAIL", fontFamily = PressStart2P, fontSize = 5.sp, color = PokedexCream.copy(0.5f)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(fontFamily = PressStart2P, fontSize = 6.sp, color = PokedexCream),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GlowBlue, unfocusedBorderColor = PokedexCream.copy(0.3f))
                )
                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    label = { Text("PASSWORD", fontFamily = PressStart2P, fontSize = 5.sp, color = PokedexCream.copy(0.5f)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    textStyle = LocalTextStyle.current.copy(fontFamily = PressStart2P, fontSize = 6.sp, color = PokedexCream),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GlowBlue, unfocusedBorderColor = PokedexCream.copy(0.3f))
                )

                if (error != null) {
                    Text(error!!, fontFamily = PressStart2P, fontSize = 5.sp, color = Color.Red.copy(alpha = 0.8f))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            keyboard?.hide()
                            if (isSignUp) viewModel.signUpWithEmail(email, password)
                            else viewModel.signInWithEmail(email, password)
                        },
                        enabled = !loading && email.isNotBlank() && password.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = GlowBlue)
                    ) {
                        if (loading) CircularProgressIndicator(Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text(if (isSignUp) "CREATE" else "SIGN IN", fontFamily = PressStart2P, fontSize = 5.sp)
                    }

                    OutlinedButton(
                        onClick = { googleSignIn.startFlow() },
                        enabled = !loading,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PokedexCream)
                    ) {
                        Text("GOOGLE", fontFamily = PressStart2P, fontSize = 5.sp)
                    }
                }
            }
        }
    }
}
