package com.projet.skilllearn.view.adapters;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.projet.skilllearn.view.fragments.ContentFragment;
import com.projet.skilllearn.view.fragments.LLMChatFragment;
import com.projet.skilllearn.view.fragments.NotesFragment;
import com.projet.skilllearn.view.fragments.QuizFragment;

public class CoursePagerAdapter extends FragmentStateAdapter {
    private static final String TAG = "CoursePagerAdapter";

    // Garder une référence aux fragments
    private final ContentFragment contentFragment;
    private final NotesFragment notesFragment;
    private final QuizFragment quizFragment;
    private final LLMChatFragment llmChatFragment;

    public CoursePagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        Log.d(TAG, "Création de CoursePagerAdapter");

        // Initialiser les fragments
        contentFragment = ContentFragment.getInstance();
        notesFragment = NotesFragment.getInstance();
        quizFragment = QuizFragment.getInstance();
        llmChatFragment = new LLMChatFragment();
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Log.d(TAG, "createFragment appelé pour la position: " + position);

        switch (position) {
            case 0:
                return contentFragment;
            case 1:
                return notesFragment;
            case 2:
                return quizFragment;
            case 3:
                return llmChatFragment;
            default:
                return contentFragment;
        }
    }

    @Override
    public int getItemCount() {
        return 4; // Contenu, Notes, Quiz, Assistant IA
    }
}