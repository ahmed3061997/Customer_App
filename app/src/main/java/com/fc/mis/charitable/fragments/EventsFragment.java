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
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class EventsFragment extends Fragment implements ChildEventListener, SwipeRefreshLayout.OnRefreshListener, ValueEventListener {
    public EventsFragment() {
        // Required empty public constructor
    }

    // firebase Database
    private DatabaseReference mDatabase;

    private AppCompatTextView mNoEventsTxt;
    private FloatingActionButton mActionFab;
    private RecyclerView mEventsListView;
    private ArrayList<Event> mEvents;
    private EventListAdapter mAdapter;
    private SwipeRefreshLayout mRefreshLayout;

    public EventsFragment(String ngoId) {
        // load specific ngo profile
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable final ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_events, container, false);

        mEventsListView = (RecyclerView) view.findViewById(R.id.event_fragment_recycler_view);
        mNoEventsTxt = (AppCompatTextView) view.findViewById(R.id.event_fragment_no_events_txt);
        mRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.event_fragment_refresh_layout);

        mEvents = new ArrayList<>();
        mAdapter = new EventListAdapter(getContext(), mEvents, true);

        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 1);

        mEventsListView.setLayoutManager(layoutManager);
        mEventsListView.setItemAnimator(new DefaultItemAnimator());
        mEventsListView.setAdapter(mAdapter);

        mRefreshLayout.setOnRefreshListener(this);

        mDatabase = FirebaseDatabase.getInstance().getReference().child("Events"); // All ngo's events

        //mDatabase.addChildEventListener(this);
        mDatabase.addValueEventListener(this);

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

    private ArrayList<DatabaseReference> mRefs = new ArrayList<>();

    // Value event listerner for all ngo nodes
    @Override
    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
        // remove listener
        // we will be kept updated with child event listener attached to every ngo node
        mDatabase.removeEventListener((ValueEventListener) this);

        // data snapshot of events node
        // iterate each ngo node
        for (final DataSnapshot snapshot : dataSnapshot.getChildren()) {
            // listen for add, remove, change in this ngo events data
            DatabaseReference ngoRef = snapshot.getRef();
            ngoRef.addChildEventListener(this);

            mRefs.add(ngoRef);
        }
    }

    @Override
    public void onCancelled(@NonNull DatabaseError databaseError) {
    }

    // child event listener for all events nodes under an ngo node

    // event added to an ngo node
    @Override
    public void onChildAdded(final @NonNull DataSnapshot dataSnapshot, @Nullable String s) {
        // this snapshot from an event just added
        // get parent node of it that represents the ngo
        String ngoId = dataSnapshot.getRef().getParent().getKey(); // ngo of this event

        // Load ngo name & thumb
        FirebaseDatabase.getInstance().getReference()
                .child("Users")
                .child(ngoId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot ngoProfileDataSnapshot) {
                        mRefreshLayout.setRefreshing(false); // stop refreshing

                        ngoProfileDataSnapshot.getRef().removeEventListener(this); // remove listener

                        String ngoName = ngoProfileDataSnapshot.child("org_name").getValue().toString();
                        String thumbImg = ngoProfileDataSnapshot.child("thumb_image").getValue().toString();

                        Event eventRef = loadFromSnapshot(dataSnapshot);

                        eventRef.setOrgName(ngoName);
                        eventRef.setOrgThumb(thumbImg);

                        mNoEventsTxt.setVisibility(View.GONE);

                        mEvents.add(eventRef);

                        sortList();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        databaseError.toException().printStackTrace();
                    }
                });
    }

    @Override
    public void onChildChanged(final @NonNull DataSnapshot dataSnapshot, @Nullable String s) {
        mRefreshLayout.setRefreshing(false); // stop refreshing

        // ngo event node changed
        for (DataSnapshot eventSnapshot : dataSnapshot.getChildren()) {
            Event newEvent = loadFromSnapshot(eventSnapshot);

            for (int i = 0; i < mEvents.size(); i++) {
                Event oldEvent = mEvents.get(i);

                if (oldEvent.getEventId().equals(eventSnapshot.getKey())) {
                    newEvent.setOrgName(oldEvent.getOrgName());
                    newEvent.setOrgThumb(oldEvent.getOrgThumb());

                    mEvents.set(i, newEvent);
                    mAdapter.notifyItemChanged(mEvents.indexOf(i));
                    break;
                }
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
        String eventId = dataSnapshot.getKey();
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
    public void onRefresh() {
        mEvents.clear();
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
