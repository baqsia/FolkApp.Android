package ge.baqar.gogia.goefolk.ui.favourites

import kotlinx.coroutines.InternalCoroutinesApi
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

@InternalCoroutinesApi
val favouritesModule = module {
    viewModel { FavouritesViewModel(get(), get(), get()) }
}

