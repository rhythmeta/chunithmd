package org.rhythmeta.chunithmd.core.update

class AppUpdateException(val failure: AppUpdateFailure) : Exception(failure.name)
