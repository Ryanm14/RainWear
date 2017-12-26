package me.ryanmiles.rain_wear

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.wearable.activity.WearableActivity
import android.util.Log
import android.view.View
import co.metalab.asyncawait.async
import com.google.android.gms.location.LocationServices
import im.delight.android.location.SimpleLocation
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject
import java.net.URL
import java.util.*


class MainActivity : WearableActivity() {

    val MY_PERMISSIONS_REQUEST_LOCATION = 99
    val APIKEY = "489fdbf67e4c93cfc03e0e76dad86997"

    data class Place(val latitude: Double, val longitude: Double, val name: String, val weather: Int)

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkLocationPermission()

        val mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        mFusedLocationClient.lastLocation
                .addOnSuccessListener(this) { location ->
                    // Got last known location. In some rare situations this can be null.
                    if (location != null) {
                        async {
                            progressBar.visibility = View.VISIBLE
                            title_text_view.visibility = View.GONE
                            // Release main thread and wait until text is loaded in background thread
                            val loadedText = await { doData(location.latitude, location.longitude) }
                            // Loaded successfully, come back in UI thread and show the result
                            if (loadedText == "") {
                                title_text_view.text = ""
                                text_view_rain.text = "Ryan is not Making It Rain Nearby :("
                            } else {
                                text_view_rain.text = loadedText
                            }
                            progressBar.visibility = View.GONE
                            title_text_view.visibility = View.VISIBLE
                        }

                    }
                }

        // Enables Always-on
        setAmbientEnabled()
    }

    private fun doData(latitude: Double, longitude: Double): String {
        val url = "http://api.openweathermap.org/data/2.5/find?lat=$latitude&lon=$longitude&cnt=50&appid=$APIKEY"
        Log.d("TAG", url)
        val forecastJsonStr = URL(url).readText()
        var name = ""

        try {
            val places = ArrayList<Place>()
            val jsonObject = JSONObject(forecastJsonStr)

            val jArray = jsonObject.getJSONArray("list")

            for (i in 0 until jArray.length()) {
                var jObj: JSONObject? = null
                jObj = jArray.getJSONObject(i)
                val weather = jObj!!.getJSONArray("weather").getJSONObject(0).getInt("id")


                if (weather <= 622 || weather == 701) {
                    val lat = jObj.getJSONObject("coord").getDouble("lat")
                    val lon = jObj.getJSONObject("coord").getDouble("lon")
                    places.add(Place(lat, lon, jObj.getString("name"), weather))
                }
            }
            var distance = 10000.0
            if (places.size != 0) {
                for (p in places) {
                    val tdistance: Double = SimpleLocation.calculateDistance(latitude, longitude, p.latitude, p.longitude)
                    if (tdistance < distance) {
                        distance = tdistance
                        name = p.name
                    }

                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return name
    }

    private fun checkLocationPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                AlertDialog.Builder(this)
                        .setTitle("Gps Location?")
                        .setMessage("Gps Location?")
                        .setPositiveButton("Ok", { _, _ ->
                            //Prompt the user once explanation has been shown
                            ActivityCompat.requestPermissions(this,
                                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                                    MY_PERMISSIONS_REQUEST_LOCATION)
                        })
                        .create()
                        .show()


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        MY_PERMISSIONS_REQUEST_LOCATION)
            }
            return false
        } else {
            return true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                        //Request location updates:

                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                }
                return
            }
        }
    }
}
