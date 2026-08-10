package com.ailenezareti.monitor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ailenezareti.monitor.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { checkPermissionsAndProceed() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Prefs.isPaired(this)) {
            showStatusScreen()
        } else {
            showPairingScreen()
        }
    }

    override fun onResume() {
        super.onResume()
        if (Prefs.isPaired(this)) {
            showStatusScreen() // icazə vəziyyəti dəyişmiş ola bilər, yenilə
        }
    }

    // ---------------- Cütləmə ekranı ----------------
    private fun showPairingScreen() {
        val bakedToken = BuildConfig.SERVER_DEVICE_TOKEN
        val hasBakedToken = bakedToken.isNotBlank() && bakedToken != "BURAYA_DASHBOARDDAN_ALDIGINIZ_KODU_YAZIN"

        binding.pairingGroup.visibility = android.view.View.VISIBLE
        binding.statusGroup.visibility = android.view.View.GONE

        if (hasBakedToken) {
            binding.tokenInput.visibility = android.view.View.GONE
            binding.pairingExplainer.text = getString(R.string.pairing_explainer_baked)
        } else {
            binding.tokenInput.visibility = android.view.View.VISIBLE
            binding.pairingExplainer.text = getString(R.string.pairing_explainer_manual)
        }

        binding.confirmButton.setOnClickListener {
            val token = if (hasBakedToken) bakedToken else binding.tokenEditText.text.toString().trim()
            if (token.isBlank()) {
                Toast.makeText(this, R.string.enter_code_first, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            binding.confirmButton.isEnabled = false
            CoroutineScope(Dispatchers.IO).launch {
                val ok = ApiClient.verifyToken(this@MainActivity, token)
                withContext(Dispatchers.Main) {
                    binding.confirmButton.isEnabled = true
                    if (ok) {
                        if (!hasBakedToken) Prefs.setManualToken(this@MainActivity, token)
                        Prefs.setPaired(this@MainActivity, true)
                        requestAllPermissions()
                    } else {
                        Toast.makeText(this@MainActivity, R.string.pairing_failed, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    // ---------------- İcazələr ----------------
    private fun requestAllPermissions() {
        val needed = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_CALL_LOG
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(needed.toTypedArray())
    }

    private fun checkPermissionsAndProceed() {
        // Arxa fon lokasiyası ayrıca, Android tələbi ilə (əvvəlcə foreground icazə verilməlidir)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), 100
            )
        }
        showStatusScreen()
    }

    // ---------------- Status ekranı (uşağın gördüyü) ----------------
    private fun showStatusScreen() {
        binding.pairingGroup.visibility = android.view.View.GONE
        binding.statusGroup.visibility = android.view.View.VISIBLE

        val missing = missingSetupSteps()
        if (missing.isEmpty()) {
            binding.statusMessage.text = getString(R.string.status_all_good)
            binding.setupList.visibility = android.view.View.GONE
            startBackgroundWork()
        } else {
            binding.statusMessage.text = getString(R.string.status_setup_needed)
            binding.setupList.visibility = android.view.View.VISIBLE
            binding.setupList.removeAllViews()
            missing.forEach { step -> addSetupButton(step) }
        }
    }

    private data class SetupStep(val label: String, val action: () -> Unit)

    private fun missingSetupSteps(): List<SetupStep> {
        val steps = mutableListOf<SetupStep>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            steps.add(SetupStep(getString(R.string.step_location)) { requestAllPermissions() })
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            steps.add(SetupStep(getString(R.string.step_background_location)) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), 100)
            })
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG)
            != PackageManager.PERMISSION_GRANTED
        ) {
            steps.add(SetupStep(getString(R.string.step_call_log)) { requestAllPermissions() })
        }

        return steps
    }

    private fun addSetupButton(step: SetupStep) {
        val button = com.google.android.material.button.MaterialButton(this).apply {
            text = step.label
            setOnClickListener { step.action() }
        }
        binding.setupList.addView(button)
    }

    private fun startBackgroundWork() {
        ContextCompat.startForegroundService(this, Intent(this, LocationTrackingService::class.java))
        SyncWorker.schedule(this)
    }
}
