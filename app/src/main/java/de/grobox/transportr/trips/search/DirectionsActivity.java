package de.grobox.transportr.trips.search;

import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.AppBarLayout.OnOffsetChangedListener;
import android.view.MenuItem;
import android.widget.FrameLayout;

import java.util.Date;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Inject;

import de.grobox.transportr.R;
import de.grobox.transportr.activities.TransportrActivity;
import de.grobox.transportr.locations.WrapLocation;

import static de.grobox.transportr.locations.WrapLocation.WrapType.GPS;
import static de.grobox.transportr.utils.Constants.DATE;
import static de.grobox.transportr.utils.Constants.FAV_TRIP_UID;
import static de.grobox.transportr.utils.Constants.FROM;
import static de.grobox.transportr.utils.Constants.SEARCH;
import static de.grobox.transportr.utils.Constants.TO;
import static de.grobox.transportr.utils.Constants.VIA;

@ParametersAreNonnullByDefault
public class DirectionsActivity extends TransportrActivity implements OnOffsetChangedListener {

	private final static String TAG = DirectionsActivity.class.getName();

	public final static String TASK_HOME = "de.grobox.transportr.HOME";
	public final static String TASK_WORK = "de.grobox.transportr.WORK";

	@Inject ViewModelProvider.Factory viewModelFactory;

	@Nullable
	private TripsFragment tripsFragment;
	private DirectionsViewModel viewModel;

	private FrameLayout fragmentContainer;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getComponent().inject(this);
		setContentView(R.layout.activity_directions);

		AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
		appBarLayout.addOnOffsetChangedListener(this);

		// get view model and observe data
		viewModel = ViewModelProviders.of(this, viewModelFactory).get(DirectionsViewModel.class);
		viewModel.showTrips().observe(this, v -> showTrips());

		fragmentContainer = findViewById(R.id.fragmentContainer);

		if (savedInstanceState == null) {
			showFavorites();
			processIntent();
		}
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		if (item.getItemId() == android.R.id.home && isShowingTrips()) {
			showFavorites();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
		if (tripsFragment != null) {
			tripsFragment.setSwipeEnabled(verticalOffset == 0);
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
		processIntent();
	}

	private void showFavorites() {
		SavedSearchesFragment f = new SavedSearchesFragment();
		getSupportFragmentManager()
				.beginTransaction()
				.replace(R.id.fragmentContainer, f, SavedSearchesFragment.TAG)
				.commit();
	}

	private void showTrips() {
		tripsFragment = new TripsFragment();
		getSupportFragmentManager().beginTransaction()
				.replace(R.id.fragmentContainer, tripsFragment, TripsFragment.TAG)
				.commit();
		fragmentContainer.requestFocus();
	}

	boolean isShowingTrips() {
		return fragmentIsVisible(TripsFragment.TAG);
	}

	private void processIntent() {
		Intent intent = getIntent();
		if (intent == null) return;

		WrapLocation from, via, to;
		boolean search;
		Date date;
		long uid = intent.getLongExtra(FAV_TRIP_UID, 0);
		String special = (String) intent.getSerializableExtra("special");
		if (special != null) {
			from = new WrapLocation(GPS);
			search = true;
			switch (special) {
				case TASK_HOME:
					to = viewModel.getHome().getValue();
					break;
				case TASK_WORK:
					to = viewModel.getWork().getValue();
					break;
				default:
					throw new IllegalArgumentException();
			}
		} else {
			from = (WrapLocation) intent.getSerializableExtra(FROM);
			to = (WrapLocation) intent.getSerializableExtra(TO);
			search = intent.getBooleanExtra(SEARCH, false);
		}
		via = (WrapLocation) intent.getSerializableExtra(VIA);
		date = (Date) intent.getSerializableExtra(DATE);

		if (search) searchFromTo(uid, from, via, to, date);
		else presetFromTo(uid, from, via, to, date);

		// remove the intent (and clear its action) since it was already processed
		// and should not be processed again
		setIntent(null);
	}

	private void presetFromTo(long uid, @Nullable WrapLocation from, @Nullable WrapLocation via, @Nullable WrapLocation to, @Nullable Date date) {
		viewModel.setFavTripUid(uid);
		if (from != null && from.getWrapType() == GPS) {
			// TODO
//			activateGPS();
			viewModel.setFromLocation(null);
		} else {
			viewModel.setFromLocation(from);
		}
		viewModel.setViaLocation(via);
		viewModel.setToLocation(to);

		if (date != null) {
			viewModel.setDate(date);
		}
	}

	private void searchFromTo(long uid, WrapLocation from, @Nullable WrapLocation via, WrapLocation to, Date date) {
		presetFromTo(uid, from, via, to, date);
		viewModel.search();
	}

}
