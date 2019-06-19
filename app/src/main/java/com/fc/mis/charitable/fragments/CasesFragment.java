package com.fc.mis.charitable.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.fc.mis.charitable.R;
import com.fc.mis.charitable.adapters.CaseListAdapter;
import com.fc.mis.charitable.models.Case;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class CasesFragment extends Fragment implements ChildEventListener, SwipeRefreshLayout.OnRefreshListener {
    public CasesFragment() {
        // Required empty public constructor
    }

    public CasesFragment(String ngoId) {
        mNgoId = ngoId;
        mNested = true;
    }

    private boolean mNested = false;
    private String mNgoId = null;

    // firebase Database
    private DatabaseReference mDatabase;

    private AppCompatTextView mNoCasesTxt;
    private FloatingActionButton mActionFab;
    private RecyclerView mCasesListView;
    private ArrayList<Case> mCases;
    private CaseListAdapter mAdapter;
    private SwipeRefreshLayout mRefreshLayout;

    private String mOrgName;
    private String mOrgThumb;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable final ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_cases, container, false);

        mCasesListView = (RecyclerView) view.findViewById(R.id.case_fragment_recycler_view);
        mNoCasesTxt = (AppCompatTextView) view.findViewById(R.id.case_fragment_no_cases_txt);
        mRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.case_fragment_refresh_layout);

        mCases = new ArrayList<>();
        mAdapter = new CaseListAdapter(getContext(), mCases, false, mNested);

        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 1);

        mCasesListView.setLayoutManager(layoutManager);
        mCasesListView.setItemAnimator(new DefaultItemAnimator());
        mCasesListView.setAdapter(mAdapter);

        mRefreshLayout.setOnRefreshListener(this);

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if (mNested)
            currentUserId = mNgoId;

        mDatabase = FirebaseDatabase.getInstance().getReference().child("Cases").child(currentUserId);

        mDatabase.addChildEventListener(this);

        mCasesListView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (mActionFab == null)
                    return;

                if (dy > 0) {
                    mActionFab.hide();
                } else if (dy < 0) {
                    mActionFab.show();
                }
            }
        });
        return view;
    }

    private void sortList() {
        Collections.sort(mCases, new Comparator<Case>() {
            @Override
            public int compare(Case o1, Case o2) {
                if (o1.getTimestamp() > o2.getTimestamp())
                    return -1;
                else {
                    return 1;
                }
            }
        });

        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
        mRefreshLayout.setRefreshing(false); // stop refreshing

        Case caseRef = loadFromSnapshot(dataSnapshot);

        mNoCasesTxt.setVisibility(View.GONE);

        mCases.add(caseRef);

        sortList();
    }

    @Override
    public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
        mRefreshLayout.setRefreshing(false); // stop refreshing

        Case newCase = loadFromSnapshot(dataSnapshot);

        for (int i = 0; i < mCases.size(); i++) {
            Case oldCase = mCases.get(i);

            if (oldCase.getCaseId().equals(dataSnapshot.getKey())) {
                mCases.set(i, newCase);
                mAdapter.notifyItemChanged(mCases.indexOf(i));
                break;
            }
        }

        sortList();
    }

    private Case loadFromSnapshot(DataSnapshot dataSnapshot) {
        Case caseRef = new Case();

        caseRef.setCaseId(dataSnapshot.getKey());

        caseRef.setTitle(dataSnapshot.child("title").getValue().toString());
        caseRef.setBody(dataSnapshot.child("body").getValue().toString());
        caseRef.setTimestamp(Long.valueOf(dataSnapshot.child("timestamp").getValue().toString()));
        caseRef.setNeeded(Integer.valueOf(dataSnapshot.child("needed").getValue().toString()));
        caseRef.setDonated(Integer.valueOf(dataSnapshot.child("donated").getValue().toString()));
        caseRef.setOrgName(mOrgName);
        caseRef.setOrgThumb(mOrgThumb);

        if (dataSnapshot.hasChild("thumb_img"))
            caseRef.setThumbImg(dataSnapshot.child("thumb_img").getValue().toString());

        if (dataSnapshot.hasChild("images")) {
            ArrayList<String> images = new ArrayList();
            for (DataSnapshot url : dataSnapshot.child("images").getChildren()) {
                images.add(url.getValue().toString());
            }
            caseRef.setImages(images);
        }

        return caseRef;
    }

    @Override
    public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
        final String caseId = dataSnapshot.getKey();
        for (Case caseRef : mCases) {
            if (caseRef.getCaseId().equals(caseId)) {
                int i = mCases.indexOf(caseRef);
                mCases.remove(i);
                mAdapter.notifyItemRemoved(i);
                break;
            }
        }

        sortList();

        if (mCases.size() == 0)
            mNoCasesTxt.setVisibility(View.VISIBLE);
    }

    @Override
    public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
    }

    @Override
    public void onCancelled(@NonNull DatabaseError databaseError) {
    }

    @Override
    public void onRefresh() {
        mCases.clear();
        mAdapter.notifyDataSetChanged();

        mDatabase.removeEventListener(this);
        mDatabase.addChildEventListener(this);
    }
}
