package com.projet.skilllearn.utils;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.projet.skilllearn.R;
import com.projet.skilllearn.view.CourseDetailActivity;

/**
 * Helper class to display a dialog when a course is created
 */
public class CourseCreatedDialogHelper {
    
    /**
     * Show a dialog when a course is created
     * @param context the context
     * @param courseId the created course ID
     */
    public static void showCourseCreatedDialog(Context context, String courseId) {
        // Inflate the dialog view
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_course_created, null);
        
        // Set the course ID
        TextView courseIdTextView = dialogView.findViewById(R.id.courseIdTextView);
        courseIdTextView.setText("ID: " + courseId);
        
        // Create the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setView(dialogView)
                .setCancelable(true);
        
        // Create and show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();
        
        // Set up the OK button
        Button okButton = dialogView.findViewById(R.id.okButton);
        okButton.setOnClickListener(v -> dialog.dismiss());
        
        // Set up the View Course button
        Button viewCourseButton = dialogView.findViewById(R.id.viewCourseButton);
        viewCourseButton.setOnClickListener(v -> {
            // Open the course detail activity
            Intent intent = new Intent(context, CourseDetailActivity.class);
            intent.putExtra("courseId", courseId);
            context.startActivity(intent);
            
            // Dismiss the dialog
            dialog.dismiss();
        });
    }
} 