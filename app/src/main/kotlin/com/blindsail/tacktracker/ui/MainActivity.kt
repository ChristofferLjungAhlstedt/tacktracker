package com.blindsail.tacktracker.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import com.blindsail.tacktracker.R

/** Placeholder screen. Real screens arrive in later milestones. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Text(stringResource(R.string.placeholder_title)) }
    }
}
