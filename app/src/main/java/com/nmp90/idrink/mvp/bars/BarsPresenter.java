package com.nmp90.idrink.mvp.bars;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.nmp90.idrink.api.Api;
import com.nmp90.idrink.api.models.Bar;
import com.nmp90.idrink.api.models.LatLng;
import com.nmp90.idrink.mvp.BasePresenter;
import com.nmp90.idrink.utils.Constants;
import com.nmp90.idrink.utils.LocationUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by joro on 05.08.16.
 */

public class BarsPresenter extends BasePresenter<BarsContract.View> implements BarsContract.Presenter,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private final Context context;
    private Api iDrinkApi;
    private GoogleApiClient googleApiClient;

    private BarsContract.View view;
    private Location currentLocation;

    @Inject
    public BarsPresenter(Context context, Api iDrinkApi, BarsContract.View view) {
        this.iDrinkApi = iDrinkApi;
        this.view = view;
        this.context = context;

        googleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Inject
    void setupListeners() {
        view.setPresenter(this);
    }

    @Override
    public void start() {
        googleApiClient.connect();
    }

    @Override
    public void stop() {
        googleApiClient.disconnect();
    }

    @Override
    public void loadBars() {
        if(currentLocation != null) {
            iDrinkApi.getBars(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), 5000, "bar", Constants.KEY)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(bars -> {
                        if(view.isActive()) {

                            List<Bar> results = bars.getResults();
                            if (results == null || results.size() == 0) {
                                view.displayNoBars();
                                return;
                            }

                            setDistanceForBars(results);
                            sortBarsByClosest(results);

                            view.displayBars(results);
                        }
                    }, err -> {
                        Timber.e("Error loading bars", err);
                    });
        } else {
            // TODO: Load all locations
        }
    }

    private void setDistanceForBars(List<Bar> results) {
        for (int i = 0; i < results.size(); i++) {
            Bar bar = results.get(i);
            double barLat = bar.getGeometry().getLocation().getLat();
            double barLng = bar.getGeometry().getLocation().getLng();

            results.get(i).setDistance(LocationUtils.getDistanceFromLatLonInKm(
                    barLat, barLng, currentLocation.getLatitude(), currentLocation.getLongitude()
            ));
        }
    }

    private void sortBarsByClosest(List<Bar> results) {
        Collections.sort(results, (bar, t1) ->
                bar.getDistance() < t1.getDistance() ? -1 : 1);
    }

    @Override
    public Location getLocation() {
        if (!isLocationGranted()) {
            view.requestLocationPermissionFromUser();
            return null;
        }

        Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        if (lastLocation != null) {
            currentLocation = lastLocation;
        }

        return currentLocation;
    }

    @Override
    public void locationApproved() {
        getLocation();
    }

    @Override
    public void locationDeclined() {
        // TODO: Load all bars available
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        getLocation();
        loadBars();
    }

    private boolean isLocationGranted() {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onConnectionSuspended(int i) {
        Timber.e("Connection Suspended!");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Timber.e("Connection Failed!" + connectionResult.getErrorMessage());
    }
}
