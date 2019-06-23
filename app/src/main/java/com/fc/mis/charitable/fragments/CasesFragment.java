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
import com.fc.mis.charitable.models.Event;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class CasesFragment extends Fragment implements ChildEventListener, SwipeRefreshLayout.OnRefreshListener, ValueEventListener {
    public CasesFragment() {
        // Required empty public constructor
    }

    public CasesFragment(String ngoId) {
        mNgoId = ngoId;
        mOneNgo = true;
    }

    private String mNgoId;
    private boolean mOneNgo = false;

    // firebase Database
    private DatabaseReference mDatabase;

    private AppCompatTextView mNoCasesTxt;
    private RecyclerView mCasesListView;
    private ArrayList<Case> mCases;
    private CaseListAdapter mAdapter;
    private SwipeRefreshLayout mRefreshLayout;
    private ArrayList<DatabaseReference> mRefs;

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
        mAdapter = new CaseListAdapter(getContext(), mCases, !mOneNgo);

        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 1);

        mCasesListView.setLayoutManager(layoutManager);
        mCasesListView.setItemAnimator(new DefaultItemAnimator());
        mCasesListView.setAdapter(mAdapter);

        mRefreshLayout.setOnRefreshListener(this);

        mDatabase = FirebaseDatabase.getInstance().getReference().child("Cases");

        if (mOneNgo)
            mDatabase = mDatabase.child(mNgoId); // down to an ngo node

        mRefs = new ArrayList<>();
        mDatabase.addValueEventListener(this);

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

    // Value event listerner for all ngo nodes
    @Override
    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
        // remove listener
        // we will be kept updated with child event listener attached to every ngo node
        mDatabase.removeEventListener((ValueEventListener) this);

        // in case of a targeted ngo
        if (mOneNgo) {
            DatabaseReference ngoRef = dataSnapshot.getRef();
            ngoRef.addChildEventListener(this);

            mRefs.add(ngoRef);
            return;
        }

        // data snapshot of events node
        // iterate each ngo node
        for (final DataSnapshot snapshot : dataSnapshot.getChildren()) {
            // listen for add, remove, change in this ngo cases data
            DatabaseReference ngoRef = snapshot.getRef();
            ngoRef.addChildEventListener(this);

            mRefs.add(ngoRef);
        }
    }

    @Override
    public void onCancelled(@NonNull DatabaseError databaseError) {
    }

    // case added to an ngo node
    @Override
    public void onChildAdded(final @NonNull DataSnapshot dataSnapshot, @Nullable String s) {
        // this snapshot of a case just added

        final String ngoId; // ngo of this event

        if (mOneNgo) // Ngo id is already provided
            ngoId = mNgoId;
        else // get parent node of it that represents the ngo
            ngoId = dataSnapshot.getRef().getParent().getKey();

        // Load ngo name & thumb
        FirebaseDatabase.getInstance().getReference()
                .child("Users")
                .child("Ngos")
                .child(ngoId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot ngoProfileDataSnapshot) {
                        mRefreshLayout.setRefreshing(false); // stop refreshing

                        ngoProfileDataSnapshot.getRef().removeEventListener(this); // remove listener

                        String ngoName = ngoProfileDataSnapshot.child("org_name").getValue().toString();
                        String thumbImg = ngoProfileDataSnapshot.child("thumb_image").getValue().toString();

                        Case caseRef = loadFromSnapshot(dataSnapshot);

                        caseRef.setNgoId(ngoId);
                        caseRef.setOrgName(ngoName);
                        caseRef.setOrgThumb(thumbImg);

                        mNoCasesTxt.setVisibility(View.GONE);

                        mCases.add(caseRef);

                        sortList();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        databaseError.toException().printStackTrace();
                    }
                });
    }

    @Override
    public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
        mRefreshLayout.setRefreshing(false); // stop refreshing

        Case newCase = loadFromSnapshot(dataSnapshot);

        for (int i = 0; i < mCases.size(); i++) {
            Case oldCase = mCases.get(i);

            if (oldCase.getCaseId().equals(dataSnapshot.getKey())) {
                newCase.setNgoId(oldCase.getNgoId());
                newCase.setOrgName(oldCase.getOrgName());
                newCase.setOrgThumb(oldCase.getOrgThumb());

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
    public void onRefresh() {
        mCases.clear();
        mAdapter.notifyDataSetChanged();

        // remove listener for each ngo node, will be attached again to reload data
        for (DatabaseReference ref : mRefs) {
            ref.removeEventListener((ChildEventListener) this);
            mRefs.remove(ref);
        }

        // reload data
        mDatabase.addValueEventListener(this);
    }
}
