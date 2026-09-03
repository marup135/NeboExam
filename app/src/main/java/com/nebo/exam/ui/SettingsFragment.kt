package com.nebo.exam.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.nebo.exam.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val PREF_SETTINGS = "nebo_settings_pref"
        private const val KEY_DARK_MODE = "key_dark_mode"
        private const val KEY_DARK_MODE_ALT = "dark_mode"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences(PREF_SETTINGS, Context.MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean(KEY_DARK_MODE, false) || prefs.getBoolean(KEY_DARK_MODE_ALT, false)

        binding.switchDarkMode.isChecked = isDarkMode

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            val currentMode = prefs.getBoolean(KEY_DARK_MODE, false) || prefs.getBoolean(KEY_DARK_MODE_ALT, false)
            if (currentMode != isChecked) {
                prefs.edit()
                    .putBoolean(KEY_DARK_MODE, isChecked)
                    .putBoolean(KEY_DARK_MODE_ALT, isChecked)
                    .apply()

                val targetMode = if (isChecked) {
                    AppCompatDelegate.MODE_NIGHT_YES
                } else {
                    AppCompatDelegate.MODE_NIGHT_NO
                }
                if (AppCompatDelegate.getDefaultNightMode() != targetMode) {
                    AppCompatDelegate.setDefaultNightMode(targetMode)
                }
            }
        }

        binding.rowTechnicalGuide.setOnClickListener {
            showTechnicalGuideDialog()
        }

        binding.rowAboutApp.setOnClickListener {
            showAboutDialog()
        }
    }

    private fun showTechnicalGuideDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Aturan & Sanksi Pelanggaran")
            .setMessage(
                "Skema Sanksi Penguncian Perangkat Bertingkat:\n\n" +
                "• Pelanggaran ke-1:\n  Penguncian layar selama 5 Menit.\n\n" +
                "• Pelanggaran ke-2:\n  Penguncian layar selama 15 Menit.\n\n" +
                "• Pelanggaran ke-3:\n  Penguncian layar selama 30 Menit.\n\n" +
                "• Pelanggaran > 3 kali:\n  Penguncian layar maksimal selama 1 Jam (60 Menit).\n\n" +
                "Catatan:\n" +
                "Tindakan keluar dari aplikasi, membuka recent apps, split screen, atau kehilangan fokus jendela secara otomatis terhitung sebagai pelanggaran."
            )
            .setPositiveButton("Saya Mengerti") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Tentang NeboExam")
            .setMessage(
                "Nama Aplikasi: NeboExam\n" +
                "Versi: 1.0.0 (beta)\n\n" +
                "Instansi:\n" +
                "SMKN 1 Bojong Purwakarta\n\n" +
                "Pengembang:\n" +
                "Ma'rup, Siswa SMKN 1 Bojong Jurusan PPLG Taruna 10\n\n" +
                "Keterangan:\n" +
                "Sistem ujian berbasis Android dengan proteksi kiosk bertingkat dan enkripsi QR AES-256."
            )
            .setPositiveButton("Tutup") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
