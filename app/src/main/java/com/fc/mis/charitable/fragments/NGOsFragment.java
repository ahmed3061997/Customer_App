package com.fc.mis.charitable.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.fc.mis.charitable.R;
import com.fc.mis.charitable.activities.MainActivity;
import com.fc.mis.charitable.adapters.NgoListAdapter;
import com.fc.mis.charitable.models.Ngo;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class NGOsFragment extends Fragment implements ChildEventListener {

    private RecyclerView mNgoListView;
    private List<Ngo> mNgos;
    private NgoListAdapter mAdapter;

    private DatabaseReference mDatabase;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_ngos, container, false);

        mNgoListView = (RecyclerView) view.findViewById(R.id.ngos_fragment_recycler_view);
        mNgos = new ArrayList<>();
        mAdapter = new NgoListAdapter(getContext(), mNgos);

        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);

        mNgoListView.setLayoutManager(layoutManager);
        mNgoListView.setItemAnimator(new DefaultItemAnimator());
        mNgoListView.setAdapter(mAdapter);

        mDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Ngos");

        // add ngos
        mDatabase.addChildEventListener(this);

        return view;
    }

    private Ngo loadNgo(DataSnapshot dataSnapshot) {
        Ngo ngo = new Ngo();
        ngo.setId(dataSnapshot.getKey());

        ngo.setOrgName(dataSnapshot.child("org_name").getValue().toString());

        ngo.setAdminName(dataSnapshot.child("first_name").getValue().toString() + " "
                + dataSnapshot.child("last_name").getValue().toString());

        ngo.setOrgAddress(dataSnapshot.child("org_address").getValue().toString());

        ngo.setThumbImage(dataSnapshot.child("thumb_image").getValue().toString());

        if (dataSnapshot.hasChild("cases_num"))
            ngo.setCasesCount(dataSnapshot.child("cases_num").getValue(Integer.class));

        if (dataSnapshot.hasChild("events_num"))
            ngo.setEventsCount(dataSnapshot.child("events_num").getValue(Integer.class));

        return ngo;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Set title bar
        ((MainActivity) getActivity()).setActionBarTitle("Organization");
        ((MainActivity) getActivity()).setActionBarShadow(true);
    }

    @Override
    public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
        Ngo ngo = loadNgo(dataSnapshot);
        mNgos.add(ngo);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
        Ngo newNgo = loadNgo(dataSnapshot);

        for (int i = 0; i < mNgos.size(); i++) {
            Ngo oldCase = mNgos.get(i);

            if (oldCase.getId().equals(dataSnapshot.getKey())) {
                mNgos.set(i, newNgo);
                mAdapter.notifyItemChanged(mNgos.indexOf(i));
                break;
            }
        }
    }

    @Override
    public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
    }

    @Override
    public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
    }

    @Override
    public void onCancelled(@NonNull DatabaseError databaseError) {
        databaseError.toException().printStackTrace();
    }
}
