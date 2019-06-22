package com.fc.mis.charitable.adapters;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.fc.mis.charitable.R;
import com.fc.mis.charitable.activities.NgoProfileActivity;
import com.fc.mis.charitable.activities.NgoProfileExtentedActivity;
import com.fc.mis.charitable.models.Ngo;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class NgoListAdapter extends RecyclerView.Adapter<NgoListAdapter.NgoViewHolder> {
    private Context mContext;
    private List<Ngo> mNgos;

    public NgoListAdapter(Context context, List<Ngo> cases) {
        this.mContext = context;
        this.mNgos = cases;
    }

    @NonNull
    @Override
    public NgoListAdapter.NgoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View viewItem = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.ngo_single_layout, parent, false);

        return new NgoListAdapter.NgoViewHolder(viewItem);
    }

    @Override
    public void onBindViewHolder(@NonNull NgoListAdapter.NgoViewHolder holder, int position) {
        final Ngo ngoItem = mNgos.get(position);
        holder.bindNgo(ngoItem);
    }

    @Override
    public int getItemCount() {
        return mNgos.size();
    }

    public class NgoViewHolder extends RecyclerView.ViewHolder {

        private MaterialCardView mCardView;
        private AppCompatTextView mOrgName;
        private CircleImageView mThumbImg;
        private Chip mCasesChip;
        private Chip mEventsChip;
        private LinearLayoutCompat mContentLayout;
        private Ngo mNgo;

        public NgoViewHolder(@NonNull View itemView) {
            super(itemView);

            mCardView = itemView.findViewById(R.id.ngo_single_card_view);
            mOrgName = itemView.findViewById(R.id.ngo_single_org_name);
            mThumbImg = itemView.findViewById(R.id.ngo_single_thumb_img);
            mCasesChip = itemView.findViewById(R.id.ngo_single_cases_chip);
            mEventsChip = itemView.findViewById(R.id.ngo_single_events_chip);

            mContentLayout = (LinearLayoutCompat) itemView.findViewById(R.id.ngo_single_content_layout);

            mContentLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showNgo();
                }
            });

            mContentLayout.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    showMenu();
                    return false;
                }
            });
        }

        private void bindNgo(Ngo ngo) {
            mNgo = ngo;

            mOrgName.setText(ngo.getOrgName());

            int casesNum = ngo.getCasesCount();
            if (casesNum != 0) {
                mCasesChip.setText(casesNum + (casesNum == 0 ? " Case" : " Cases"));
                mCasesChip.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showDetails("Cases");
                    }
                });
            } else {
                mCasesChip.setVisibility(View.GONE);
            }

            int eventsNum = ngo.getEventsCount();
            if (eventsNum != 0) {
                mEventsChip.setText(eventsNum + (eventsNum == 0 ? " Event" : " Events"));
                mEventsChip.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showDetails("Events");
                    }
                });
            } else {
                mEventsChip.setVisibility(View.GONE);
            }

            Picasso.get().load(ngo.getThumbImage()).placeholder(R.drawable.default_avatar).into(mThumbImg);
        }

        private void showNgo() {
            Intent intent = new Intent(mContext, NgoProfileActivity.class);
            intent.putExtra("Ngo", mNgo);

            Activity activity = ((Activity) mContext);
            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    activity,
                    mThumbImg,
                    "ThumbImage");

            if (mNgo.getThumbImage().equals("default"))
                mContext.startActivity(intent);
            else
                mContext.startActivity(intent, options.toBundle());
        }

        private void showDetails(String catagory) {
            Intent intent = new Intent(mContext, NgoProfileExtentedActivity.class);
            intent.putExtra("ViewType", catagory);
            intent.putExtra("NgoId", mNgo.getId());
            intent.putExtra("NgoName", mNgo.getOrgName());
            mContext.startActivity(intent);
        }

        public void showMenu() {
            AlertDialog.Builder builder = new AlertDialog.Builder(itemView.getContext());

            builder.setItems(new String[]{"Send Message", "View Profile", "View Cases", "View Events"}, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == 0) {
                        // TODO: send message activity
                    } else if (which == 1) {
                        showNgo();
                    } else if (which == 2) {
                        showDetails("Cases");
                    } else if (which == 3) {
                        showDetails("Events");
                    }
                }
            });

            builder.create().show();
        }
    }
}
