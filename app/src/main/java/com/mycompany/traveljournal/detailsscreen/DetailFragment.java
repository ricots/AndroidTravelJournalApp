package com.mycompany.traveljournal.detailsscreen;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ToxicBakery.viewpager.transforms.CubeOutTransformer;
import com.mycompany.traveljournal.R;
import com.mycompany.traveljournal.base.ImageAdapter;
import com.mycompany.traveljournal.base.TravelBaseFragment;
import com.mycompany.traveljournal.commentscreen.CommentsAdapter;
import com.mycompany.traveljournal.helpers.BitmapScaler;
import com.mycompany.traveljournal.helpers.DeviceDimensionsHelper;
import com.mycompany.traveljournal.helpers.Util;
import com.mycompany.traveljournal.mapscreen.SingleMapActivity;
import com.mycompany.traveljournal.models.Comment;
import com.mycompany.traveljournal.models.Post;
import com.mycompany.traveljournal.service.JournalApplication;
import com.mycompany.traveljournal.service.JournalCallBack;
import com.mycompany.traveljournal.service.JournalService;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;


public class DetailFragment extends TravelBaseFragment {

    private final static String TAG = "DetailFragment";
    private final static int numComments = 3;
    private String postId;
    private ImageView ivProfile;
    //private ImageView ivPost;
    private TextView tvCaption;
    private ImageView ivShare;
    private ImageView ivSharePhoto;
    private ImageView ivFollow;
    private ImageView ivStar;
    private ImageView ivComment;
    private TextView tvLikes;
    private TextView tvName;
    private Toolbar toolbar;

    private ImageView ivStaticMap;
    private Post m_post;
    private TextView tvNumComments;
    private String localPhotoPath;
    private boolean imageViewLoaded = false;
    private ViewPager viewPager;
    com.nirhart.parallaxscroll.views.ParallaxScrollView parallaxScrollView;

    private CommentsAdapter aComments;
    private LinearLayout llComments;
    JournalService client;
    private ArrayList<String> images = new ArrayList<>();
    private boolean isShareEnabled = true;

    private OpenCommentsListenerInterface openCommentsListener;

    public static DetailFragment newInstance(String postId, String localPhotoPath) {
        DetailFragment detailFragment = new DetailFragment();
        Bundle args = new Bundle();
        args.putString("post_id", postId);
        args.putString("local_photo_path", localPhotoPath);
        detailFragment.setArguments(args);
        return detailFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_post_detail, container, false);
        setUpViews(view);
        setToolbar();
        setUpListeners();
        populateImageViewFromLocal();
        fetchPostAndPopulateViews();
        return view;
    }

    public void setUpViews(View v){
        ivProfile = (ImageView) v.findViewById(R.id.ivProfile);
        //ivPost = (ImageView) v.findViewById(R.id.ivPost);
        tvCaption = (TextView) v.findViewById(R.id.tvCaption);
        //ivShare = (ImageView) v.findViewById(R.id.ivShare);
        ivSharePhoto = (ImageView) v.findViewById(R.id.ivSharePhoto);
        ivFollow = (ImageView) v.findViewById(R.id.ivFollow);
        ivStar = (ImageView) v.findViewById(R.id.ivStar);
        ivComment = (ImageView) v.findViewById(R.id.ivComment);
        tvLikes = (TextView) v.findViewById(R.id.tvLikes);
        tvName = (TextView) v.findViewById(R.id.tvUserName);
        toolbar = (Toolbar) v.findViewById(R.id.toolbar);
        ivStaticMap = (ImageView)v.findViewById(R.id.ivStaticMap);
        tvNumComments = (TextView) v.findViewById(R.id.tvNumComments);
        llComments = (LinearLayout) v.findViewById(R.id.llComments);

        viewPager = (ViewPager) v.findViewById(R.id.view_pager);
        viewPager.setPageTransformer(true, new CubeOutTransformer());

        parallaxScrollView = (com.nirhart.parallaxscroll.views.ParallaxScrollView) v.findViewById(R.id.parallaxScrollView);
        tvName = (TextView) v.findViewById(R.id.tvName);

        super.setUpViews(v);
    }

    public void setUpListeners() {

        //Once clicked to map, go to single map activity for that post
        ivStaticMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getActivity(), SingleMapActivity.class);
                i.putExtra("post_id", postId);
                startActivity(i);
                getActivity().overridePendingTransition(R.anim.right_in, R.anim.left_out);
            }
        });

        ivComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCommentsListener.openCommentsScreen(postId);
            }
        });

        /*ivShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sharePost();
            }
        });*/

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_detail, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                getActivity().supportFinishAfterTransition();
                return true;
            case R.id.action_share:
                sharePost();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        postId = getArguments().getString("post_id", "");
        localPhotoPath = getArguments().getString("local_photo_path");

        Log.wtf(TAG, "post_id is : " + postId + " , local_photo_path is : " + localPhotoPath);

        client = JournalApplication.getClient();
    }

    private void fetchPostAndPopulateViews() {

        showProgress();

        client.getPostWithId(postId, new JournalCallBack<Post>() {
            @Override
            public void onSuccess(Post post) {
                m_post = post;
                populateViews(post);
                fetchAndPopulateComments(post);
                hideProgress();
            }

            @Override
            public void onFailure(Exception e) {
                Log.wtf(TAG, "Post not found with id " + postId);
                hideProgress();
            }

        });
    }


    private void populateViews(Post post) {
        if(!imageViewLoaded) {
            images.add(post.getImageUrl());
            //TODO get hardcoded image list from parse
            //images.add("https://canadaalive.files.wordpress.com/2013/03/2789604382_1920dbcc87.jpg");
            //images.add("http://www.specialswallpaper.com/wp-content/uploads/2015/04/free_high_resolution_images_for_download-1.jpg");
            //images.add("http://www.hdwallpapersimages.com/wp-content/uploads/2014/01/Winter-Tiger-Wild-Cat-Images.jpg");
            if(post.getImage1Url() != null)
                images.add(post.getImage1Url());

            if(post.getImage2Url() != null)
                images.add(post.getImage2Url());

            if(post.getImage3Url() != null)
                images.add(post.getImage3Url());

            ImageAdapter adapter = new ImageAdapter(getActivity(), images, null);
            viewPager.setAdapter(adapter);
         }

        tvCaption.setText(post.getCaption());
        tvLikes.setText(post.getLikes()+"");
        tvName.setText(post.getParseUser().getName());

        // Default profile picture
        Picasso.with(getActivity()).load(R.drawable.icon_user_32)
                .fit()
                .centerCrop()
                .placeholder(R.drawable.placeholderthumbnail)
                .transform(Util.getTransformation())
                .into(ivProfile);

        if(post.getParseUser()!=null) {
            Picasso.with(getActivity()).load(post.getParseUser().getProfileImgUrl())
                    .fit()
                    .centerCrop()
                    .placeholder(R.drawable.placeholderthumbnail)
                    .transform(Util.getTransformation())
                    .into(ivProfile);
        }

        //put static map
        double latitude = post.getLatitude();
        double longitude = post.getLongitude();
        int width = 600;
        String staticMapUrl = "https://maps.googleapis.com/maps/api/staticmap?size="
                + width + "x" + width + "&zoom=17&markers="
                //+ "icon:http://chart.apis.google.com/chart?chst=d_map_pin_icon%26chld=cafe%257C996600%7C"
                + latitude + "," + longitude;
        Picasso.with(getActivity()).load(staticMapUrl)
                .into(ivStaticMap);

        //Load image into placeholder to enable image sharing
        Picasso.with(getActivity()).load(m_post.getImageUrl()).into(ivSharePhoto);
    }

    private void populateNumComments(int numComments) {
        tvNumComments.setText(numComments+"");

    }

    private void setToolbar() {
        if (toolbar != null) {
            ((ActionBarActivity) getActivity()).setSupportActionBar(toolbar);
            //toolbar.setBackgroundColor(Color.parseColor(user.getProfileBackgroundColor()));

            // Set the home icon on toolbar
            ActionBar actionbar = ((ActionBarActivity) getActivity()).getSupportActionBar();
            actionbar.setDisplayHomeAsUpEnabled(true);
            //actionbar.setHomeAsUpIndicator(R.drawable.ic_up_menu);
        }
    }

    private void fetchAndPopulateComments(Post post) {
        client.getCommentsForPost(post.getPostID(), 1000, new JournalCallBack<List<Comment>>() {
            @Override
            public void onSuccess(List<Comment> comments) {
                Log.wtf(TAG, "Got comments #="+comments.size());
                addAllCommentsToList(comments);
                populateNumComments(comments.size());
            }

            @Override
            public void onFailure(Exception e) {
                Log.wtf(TAG, "Failed to get comments:"+e.toString());
            }
        });
    }

    public void populateImageViewFromLocal(){
        if(localPhotoPath!=null) {

            Log.d(TAG, "local_photo_path : " + localPhotoPath);
            Bitmap takenImage1 = Util.rotateBitmapOrientation(localPhotoPath);
            int screenWidth = DeviceDimensionsHelper.getDisplayWidth(getActivity());
            Bitmap localImage = BitmapScaler.scaleToFitWidth(takenImage1, screenWidth);
            ArrayList<String> fakeImg = new ArrayList<String>();
            fakeImg.add("");

            ImageAdapter adapter = new ImageAdapter(getActivity(), fakeImg , localImage);
            viewPager.setAdapter(adapter);
            imageViewLoaded = true;
        }else{
            Log.d(TAG, "local photo path is null");
        }
    }

    public void addAllCommentsToList(List<Comment> comments) {
        if(getActivity()!=null){
            ViewGroup llComments = (ViewGroup) getActivity().findViewById(R.id.llComments);

            int numCommentsToShow = numComments;
            if (comments.size() < numCommentsToShow) {
                numCommentsToShow = comments.size();
            }

            for (int i = 0 ; i < numCommentsToShow ; i++ ){
                addSingleCommentToList(comments.get(i), llComments);
            }
        }else{
            Log.d(TAG, "getActivity returns null");
        }
    }

    private void addSingleCommentToList(Comment comment, ViewGroup viewGroup) {
        LayoutInflater i = (LayoutInflater) getActivity().getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View commentDetailView = i.inflate(R.layout.fragment_comment_detail, null);

        TextView tvName = (TextView) commentDetailView.findViewById(R.id.tvName);
        TextView tvBody = (TextView) commentDetailView.findViewById(R.id.tvBody);
        ImageView ivProfileImage = (ImageView) commentDetailView.findViewById(R.id.ivProfileImage);

        tvName.setText(comment.getUser().getName());
        tvBody.setText(comment.getBody());

        ivProfileImage.setImageResource(android.R.color.transparent);
        Picasso.with(getActivity()).load(comment.getUser().getProfileImgUrl())
                .fit()
                .centerCrop()
                .placeholder(R.drawable.placeholderthumbnail)
                .transform(Util.getTransformation())
                .into(ivProfileImage);

        viewGroup.addView(commentDetailView);
    }

    public interface OpenCommentsListenerInterface {
        public void openCommentsScreen(String postId);
    }

    public void setListener(OpenCommentsListenerInterface openCommentsListener) {
        this.openCommentsListener = openCommentsListener;
    }

    public void refreshComments() {
        // Clear Existing Comments
        ViewGroup llComments = (ViewGroup) getActivity().findViewById(R.id.llComments);
        llComments.removeAllViews();

        fetchAndPopulateComments(m_post);
    }

    // Mostly copied from http://guides.codepath.com/android/Sharing-Content-with-Intents
    // Can be triggered by a view event such as a button press
    //
    // This requires the photo be loaded into an image view, hence the need for the invisible ivSharePhoto
    //
    public void sharePost() {
        if (!isShareEnabled) {
            return;
        }
        isShareEnabled = false;

        // Get access to bitmap image from view
        ImageView ivImage = (ImageView) getActivity().findViewById(R.id.ivSharePhoto);
        // Get access to the URI for the bitmap
        Uri bmpUri = Util.getLocalBitmapUri(ivImage);
        if (bmpUri != null) {
            // Construct a ShareIntent with link to image
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, bmpUri);
            shareIntent.setType("image/*");
            // Launch sharing dialog for image
            startActivity(Intent.createChooser(shareIntent, "Share Image"));
            isShareEnabled = true;
        } else {
            // ...sharing failed, handle error
            isShareEnabled = true;
        }
    }

}
