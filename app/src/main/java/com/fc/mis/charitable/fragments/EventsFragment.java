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
import com.fc.mis.charitable.adapters.EventListAdapter;
import com.fc.mis.charitable.models.Event;
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

public class EventsFragment extends Fragment implements ChildEventListener, SwipeRefreshLayout.OnRefreshListener {
    public EventsFragment() {
        // Required empty public constructor
    }

    public EventsFragment(String ngoId) {
        mNgoId = ngoId;
        mNested = true;
    }

    private boolean mNested = false;
    private String mNgoId = null;

    // firebase Database
    private DatabaseReference mDatabase;

    private AppCompatTextView mNoEventsTxt;
    private FloatingActionButton mActionFab;
    private RecyclerView mEventsListView;
    private ArrayList<Event> mEvents;
    private EventListAdapter mAdapter;
    private SwipeRefreshLayout mRefreshLayout;

    private String mOrgName;
    private String mOrgThumb;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable final ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_events, container, false);

        if (!mNested)
            mActionFab = getParentFragment().getView().findViewById(R.id.home_action_fab_btn);

        mEventsListView = (RecyclerView) view.findViewById(R.id.event_fragment_recycler_view);
        mNoEventsTxt = (AppCompatTextView) view.findViewById(R.id.event_fragment_no_events_txt);
        mRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.event_fragment_refresh_layout);

        mEvents = new ArrayList<>();
        mAdapter = new EventListAdapter(getContext(), mEvents, false, mNested);

        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 1);

        mEventsListView.setLayoutManager(layoutManager);
        mEventsListView.setItemAnimator(new DefaultItemAnimator());
        mEventsListView.setAdapter(mAdapter);

        mRefreshLayout.setOnRefreshListener(this);

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if (mNested)
            currentUserId = mNgoId;

        mDatabase = FirebaseDatabase.getInstance().getReference().child("Events").child(currentUserId);

        mDatabase.addChildEventListener(this);

        mEventsListView.addOnScrollListener(new RecyclerView.OnScrollListener() {
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
        Collections.sort(mEvents, new Comparator<Event>() {
            @Override
            public int compare(Event o1, Event o2) {
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

        Event eventRef = loadFromSnapshot(dataSnapshot);

        mNoEventsTxt.setVisibility(View.GONE);

        mEvents.add(eventRef);

        sortList();
    }

    @Override
    public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
        mRefreshLayout.setRefreshing(false); // stop refreshing

        Event newEvent = loadFromSnapshot(dataSnapshot);

        for (int i = 0; i < mEvents.size(); i++) {
            Event oldEvent = mEvents.get(i);

            if (oldEvent.getEventId().equals(dataSnapshot.getKey())) {
                mEvents.set(i, newEvent);
                mAdapter.notifyItemChanged(mEvents.indexOf(i));
                break;
            }
        }

        sortList();
    }

    private Event loadFromSnapshot(DataSnapshot dataSnapshot) {
        Event eventRef = new Event();

        eventRef.setEventId(dataSnapshot.getKey());

        eventRef.setTitle(dataSnapshot.child("title").getValue().toString());
        eventRef.setBody(dataSnapshot.child("body").getValue().toString());
        eventRef.setLocation(dataSnapshot.child("location").getValue().toString());
        eventRef.setTime(Long.valueOf(dataSnapshot.child("time").getValue().toString()));
        eventRef.setTimestamp(Long.valueOf(dataSnapshot.child("timestamp").getValue().toString()));
        eventRef.setOrgName(mOrgName);
        eventRef.setOrgThumb(mOrgThumb);

        if (dataSnapshot.hasChild("thumb_img"))
            eventRef.setThumbImg(dataSnapshot.child("thumb_img").getValue().toString());

        if (dataSnapshot.hasChild("images")) {
            ArrayList<String> images = new ArrayList();
            for (DataSnapshot url : dataSnapshot.child("images").getChildren()) {
                images.add(url.getValue().toString());
            }
            eventRef.setImages(images);
        }

        return eventRef;
    }

    @Override
    public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
        final String eventId = dataSnapshot.getKey();
        for (Event eventRef : mEvents) {
            if (eventRef.getEventId().equals(eventId)) {
                int i = mEvents.indexOf(eventRef);
                mEvents.remove(i);
                mAdapter.notifyItemRemoved(i);
                break;
            }
        }

        sortList();

        if (mEvents.size() == 0)
            mNoEventsTxt.setVisibility(View.VISIBLE);
    }

    @Override
    public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
    }

    @Override
    public void onCancelled(@NonNull DatabaseError databaseError) {
    }

    @Override
    public void onRefresh() {
        mEvents.clear();
        mAdapter.notifyDataSetChanged();

        mDatabase.removeEventListener(this);
        mDatabase.addChildEventListener(this);
    }
}
