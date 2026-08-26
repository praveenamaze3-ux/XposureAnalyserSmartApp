package com.example.xposuredetectorsmart.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.xposuredetectorsmart.ui.audit.AuditTrailScreen
import com.example.xposuredetectorsmart.ui.camera.CameraScreen
import com.example.xposuredetectorsmart.ui.components.BiometricGate
import com.example.xposuredetectorsmart.ui.dashboard.DashboardScreen
import com.example.xposuredetectorsmart.ui.qr.QRScannerScreen
import com.example.xposuredetectorsmart.ui.results.ResultsScreen
import com.example.xposuredetectorsmart.ui.settings.SettingsScreen
import com.example.xposuredetectorsmart.ui.strip.StripScannerScreen
import com.example.xposuredetectorsmart.viewmodel.DoseAnalysisViewModel
import com.example.xposuredetectorsmart.viewmodel.ShiftState
import com.example.xposuredetectorsmart.viewmodel.SharedShiftViewModel

object Routes {
    const val QR_SCANNER = "qr_scanner"
    const val STRIP_SCANNER = "strip_scanner"
    const val CAMERA = "camera"
    const val RESULTS = "results"
    const val DASHBOARD = "dashboard"
    const val SETTINGS = "settings"
    const val AUDIT_TRAIL = "audit_trail"
}

@Composable
fun H2SNavGraph(navController: NavHostController = rememberNavController()) {
    // Scoped to the Activity (default owner at this point in composition, above any
    // NavBackStackEntry) so shift context and in-flight capture results survive navigation.
    val sharedShiftViewModel: SharedShiftViewModel = hiltViewModel()
    val doseAnalysisViewModel: DoseAnalysisViewModel = hiltViewModel()

    NavHost(navController = navController, startDestination = Routes.QR_SCANNER) {
        composable(Routes.QR_SCANNER) {
            QRScannerScreen(
                sharedShiftViewModel = sharedShiftViewModel,
                onWorkerIdentified = {
                    navController.navigate(Routes.STRIP_SCANNER) {
                        popUpTo(Routes.QR_SCANNER) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.STRIP_SCANNER) {
            StripScannerScreen(
                sharedShiftViewModel = sharedShiftViewModel,
                onPaired = {
                    navController.navigate(Routes.CAMERA) {
                        popUpTo(Routes.STRIP_SCANNER) { inclusive = true }
                    }
                },
                onNeedWorker = {
                    navController.navigate(Routes.QR_SCANNER) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.CAMERA) {
            CameraScreen(
                sharedShiftViewModel = sharedShiftViewModel,
                doseAnalysisViewModel = doseAnalysisViewModel,
                onCaptured = { navController.navigate(Routes.RESULTS) },
                onNeedQr = {
                    navController.navigate(Routes.QR_SCANNER) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.RESULTS) {
            ResultsScreen(
                doseAnalysisViewModel = doseAnalysisViewModel,
                onDone = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.CAMERA) { inclusive = true }
                    }
                },
                onRetry = { navController.popBackStack() },
            )
        }

        composable(Routes.DASHBOARD) {
            val shiftState by sharedShiftViewModel.shiftState.collectAsState()
            val workerId = (shiftState as? ShiftState.Active)?.context?.workerId

            if (workerId != null) {
                BiometricGate(workerId = workerId) {
                    DashboardScreen(
                        sharedShiftViewModel = sharedShiftViewModel,
                        onNewCapture = { navController.navigate(Routes.CAMERA) },
                        onExportPdf = { navController.navigate(Routes.SETTINGS) },
                        onSettings = { navController.navigate(Routes.SETTINGS) },
                        onSwitchWorkerRequested = {
                            navController.navigate(Routes.QR_SCANNER) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                    )
                }
            } else {
                DashboardScreen(
                    sharedShiftViewModel = sharedShiftViewModel,
                    onNewCapture = { navController.navigate(Routes.CAMERA) },
                    onExportPdf = { navController.navigate(Routes.SETTINGS) },
                    onSettings = { navController.navigate(Routes.SETTINGS) },
                    onSwitchWorkerRequested = {
                        navController.navigate(Routes.QR_SCANNER) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                )
            }
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                sharedShiftViewModel = sharedShiftViewModel,
                onViewAuditTrail = { navController.navigate(Routes.AUDIT_TRAIL) },
                onLoggedOut = {
                    navController.navigate(Routes.QR_SCANNER) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.AUDIT_TRAIL) {
            AuditTrailScreen()
        }
    }
}
