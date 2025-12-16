package com.example.wordleclone

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wordleclone.ui.theme.Green
import com.example.wordleclone.ui.theme.Yellow

@Composable
fun SolverScreen(viewModel: WordleViewModel) {
    // Local state for the inputs
    var greenInput by remember { mutableStateOf(List(5) { "" }) }
    var yellowInput by remember { mutableStateOf("") }
    var grayInput by remember { mutableStateOf("") }

    val results = viewModel.solverResults

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("WORDLE SOLVER", fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))

        // 1. GREEN INPUTS (Fixed Positions)
        Text("Green Letters (Known Position)", fontSize = 14.sp, color = Green, fontWeight = FontWeight.Bold)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            for (i in 0 until 5) {
                SolverCharInput(
                    value = greenInput[i],
                    onValueChange = { char ->
                        val newList = greenInput.toMutableList()
                        // Only allow 1 char
                        if (char.length <= 1) {
                            newList[i] = char.uppercase()
                            greenInput = newList
                        }
                    },
                    color = Green
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. YELLOW INPUTS (Must Contain)
        SolverTextInput(
            label = "Yellow Letters (Must Exist)",
            value = yellowInput,
            onValueChange = { yellowInput = it },
            color = Yellow
        )

        // 3. GRAY INPUTS (Excluded)
        SolverTextInput(
            label = "Gray Letters (Excluded)",
            value = grayInput,
            onValueChange = { grayInput = it },
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ACTION BUTTONS
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = {
                    viewModel.clearSolver()
                    greenInput = List(5) {""}
                    yellowInput = ""
                    grayInput = ""
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) {
                Text("CLEAR")
            }

            Button(
                onClick = {
                    // Convert list ["A", "", "P", "", ""] -> "A_P__"
                    val greenStr = greenInput.joinToString("") { it.ifBlank { "_" } }
                    viewModel.solveWordle(greenStr, yellowInput, grayInput)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("SOLVE")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // RESULTS LIST
        Text("Matches Found: ${results.size}", fontWeight = FontWeight.Bold)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // Takes remaining space
                .padding(top = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(results.chunked(4)) { rowWords ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        rowWords.forEach { word ->
                            Text(
                                text = word,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SolverCharInput(value: String, onValueChange: (String) -> Unit, color: Color) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.2f))
            .border(2.dp, color, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxSize().wrapContentHeight()
        )
    }
}

@Composable
fun SolverTextInput(label: String, value: String, onValueChange: (String) -> Unit, color: Color) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, fontSize = 14.sp, color = color, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it.uppercase()) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = color,
                unfocusedBorderColor = color.copy(alpha = 0.5f)
            )
        )
    }
}