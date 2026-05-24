package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class PromptTemplate(
    val title: String,
    val description: String,
    val fullPrompt: String,
    val category: String,
    val icon: ImageVector
)

@Composable
fun TemplatesScreen(
    onSelectTemplate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val templates = listOf(
        PromptTemplate(
            title = "Email Polish & Warmth",
            description = "Refines awkward or direct drafts into professional yet encouraging text.",
            fullPrompt = "Please polish the following rough draft email to make it warmer, professional, and clear. Eliminate passive-aggressive phrasings:\n\n[Insert your draft here]",
            category = "Writing",
            icon = Icons.Default.Email
        ),
        PromptTemplate(
            title = "Code Optimizer & Debugger",
            description = "Analyzes a snippet of code, patches bugs, and optimizes performance.",
            fullPrompt = "Review the following block of code. Identify bugs, syntax errors, and performance bottlenecks, and rewrite a highly optimized, clean version with explanatory comments:\n\n```\n// Add your snippet here\n```",
            category = "Coding",
            icon = Icons.Default.Code
        ),
        PromptTemplate(
            title = "High-Concept Brainstorming",
            description = "Generates highly creative and unexpected naming or narrative directions.",
            fullPrompt = "I need help brainstorming creative and modern concepts. Please provide 5 unique, unexpected naming directions or product features for following criteria:\n\n- Primary Goal: \n- Target Theme: ",
            category = "Creativity",
            icon = Icons.Default.Lightbulb
        ),
        PromptTemplate(
            title = "Notes & Meeting Summarizer",
            description = "Turns raw meeting audio transcript scripts into clean KPI lists & action tags.",
            fullPrompt = "Please extract the core meeting key takeaways, major performance indicators, action items, assignees, and next milestone checkups from these transcript notes:\n\n[Paste transcripts here]",
            category = "Productivity",
            icon = Icons.Default.MenuBook
        ),
        PromptTemplate(
            title = "Complex Concept Simplifier",
            description = "Explains deep technical jargon to an eager, novice 10-year-old listener.",
            fullPrompt = "Explain the following highly complex technical concept or mathematical formula to an inquisitive 10-year-old. Use relatable metaphors and completely avoid intimidating jargon:\n\n",
            category = "Learning",
            icon = Icons.Default.School
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Upper Title Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "Prompt Commands Library",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Speed up your tasks with pre-crafted structured prompts optimized for Gemini models details.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(templates) { template ->
                TemplateItemCard(
                    template = template,
                    onClick = { onSelectTemplate(template.fullPrompt) }
                )
            }
        }
    }
}

@Composable
fun TemplateItemCard(
    template: PromptTemplate,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag("template_${template.title.lowercase().replace(" ", "_")}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStrokeAlternative(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = template.icon,
                    contentDescription = template.title,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = template.title,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = template.category,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = template.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

// Inline helper because BorderStroke might be complex for matching, keep it minimal and clean
@Composable
fun BorderStrokeAlternative(width: androidx.compose.ui.unit.Dp, color: Color): androidx.compose.foundation.BorderStroke {
    return remember(width, color) {
        androidx.compose.foundation.BorderStroke(width, color)
    }
}
