package ru.embtlab.smartprice.core.distribution

import android.app.Application

interface DistributionInitializer {
    fun initialize(application: Application)
}