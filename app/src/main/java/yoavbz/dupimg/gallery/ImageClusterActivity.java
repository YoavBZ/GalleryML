package yoavbz.dupimg.gallery;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spanned;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import com.bignerdranch.android.multiselector.MultiSelector;
import yoavbz.dupimg.R;
import yoavbz.dupimg.gallery.adapters.CustomViewPager;
import yoavbz.dupimg.gallery.adapters.HorizontalListAdapter;
import yoavbz.dupimg.gallery.adapters.ViewPagerAdapter;
import yoavbz.dupimg.models.Image;

import java.util.ArrayList;

/**
 * The type Media gallery activity.
 */
public class ImageClusterActivity extends AppCompatActivity
		implements ViewPager.OnPageChangeListener, HorizontalListAdapter.OnImageClickListener {

	protected Toolbar mToolbar;
	protected ArrayList<Image> dataSet;
	private CustomViewPager mViewPager;
	private RecyclerView imagesHorizontalList;
	private HorizontalListAdapter hAdapter;
	private MultiSelector multiSelector;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gallery);
		initValues();
		initViews();
		initAdapters();
	}

	private void initValues() {
		Intent intent = getIntent();
		if (intent == null || intent.getExtras() == null) {
			return;
		}
		Bundle bundle = intent.getExtras();
		dataSet = bundle.getParcelableArrayList("IMAGES");
	}

	private void initViews() {
		mToolbar = findViewById(R.id.toolbar_media_gallery);
		if (getSupportActionBar() != null) {
			mToolbar.setVisibility(View.GONE);
			getSupportActionBar().setTitle(R.string.cluster_activity_title);
		} else {
			setSupportActionBar(mToolbar);
			mToolbar.setTitle(R.string.cluster_activity_title);
		}
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);

		mViewPager = findViewById(R.id.pager);
		imagesHorizontalList = findViewById(R.id.horizontal_list);
	}

	public void initAdapters() {
		mViewPager.setAdapter(new ViewPagerAdapter(this, dataSet, mToolbar, imagesHorizontalList));
		multiSelector = new MultiSelector();
		multiSelector.setSelectable(true);
		hAdapter = new HorizontalListAdapter(this, dataSet, this, multiSelector);
		imagesHorizontalList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
		imagesHorizontalList.setAdapter(hAdapter);
		hAdapter.notifyDataSetChanged();
		mViewPager.addOnPageChangeListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.image_toolbar_menu, menu);
		boolean showItem = !multiSelector.getSelectedPositions().isEmpty() &&
				multiSelector.getSelectedPositions().size() != dataSet.size();
		menu.findItem(R.id.action_select).setVisible(showItem);
		return true;
	}

	/**
	 * Handles the selection of images to keep, by popping a verification dialog
	 *
	 * @param item The selected menu item
	 * @return super method return value
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_select) {
			Spanned message =
					Html.fromHtml("You chose to keep <b>" + multiSelector.getSelectedPositions().size() +
							              "</b> out of <b>" + dataSet.size() +
							              "</b> images.<br>Are you sure you want to delete the unselected images?", 0);
			new AlertDialog.Builder(this)
					.setTitle("Delete Duplicates?")
					.setMessage(message)
					.setPositiveButton("DELETE", (dialog, which) -> {
						Toast.makeText(this, "Deleting duplicates..", Toast.LENGTH_LONG).show();
						// Deleting unselected images
						new Thread(() -> {
							for (int i = 0; i < dataSet.size(); i++) {
								if (!multiSelector.isSelected(i, 0)) {
									Image image = dataSet.get(i);
									image.delete(this);
								}
							}
							setResult(RESULT_OK);
							finish();
						}).start();
					})
					.setNegativeButton("Cancel", null)
					.show();
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Behaves like onBackPressed
	 *
	 * @return true always
	 */
	@Override
	public boolean onSupportNavigateUp() {
		onBackPressed();
		return true;
	}

	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
	}

	@Override
	public void onPageSelected(int position) {
		imagesHorizontalList.smoothScrollToPosition(position);
		hAdapter.setSelectedItem(position);
	}

	@Override
	public void onPageScrollStateChanged(int state) {

	}

	@Override
	public void onImageClick(int pos) {
		mViewPager.setCurrentItem(pos, true);
	}

}
