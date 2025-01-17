package eu.kanade.tachiyomi.ui.setting

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.ISelectionListener
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import com.mikepenz.fastadapter.select.SelectExtension
import com.mikepenz.fastadapter.select.getSelectExtension
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.databinding.ThemeItemBinding
import eu.kanade.tachiyomi.databinding.ThemesPreferenceBinding
import eu.kanade.tachiyomi.util.system.ThemeUtil
import eu.kanade.tachiyomi.util.system.Themes
import eu.kanade.tachiyomi.util.system.appDelegateNightMode
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.isInNightMode
import uy.kohesive.injekt.injectLazy
import kotlin.math.max

class ThemePreference @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    Preference(context, attrs) {

    var fastAdapterLight: FastAdapter<ThemeItem>
    var fastAdapterDark: FastAdapter<ThemeItem>
    private val itemAdapterLight = ItemAdapter<ThemeItem>()
    private val itemAdapterDark = ItemAdapter<ThemeItem>()
    private var selectExtensionLight: SelectExtension<ThemeItem>
    private var selectExtensionDark: SelectExtension<ThemeItem>
    private val preferences: PreferencesHelper by injectLazy()
    var activity: Activity? = null
    var lastScrollPostionLight: Int? = null
    var lastScrollPostionDark: Int? = null
    lateinit var binding: ThemesPreferenceBinding
    private val managerLight = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
    private val managerDark = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
    init {
        layoutResource = R.layout.themes_preference
        fastAdapterLight = FastAdapter.with(itemAdapterLight)
        fastAdapterDark = FastAdapter.with(itemAdapterDark)
        fastAdapterLight.setHasStableIds(true)
        fastAdapterDark.setHasStableIds(true)
        selectExtensionLight = fastAdapterLight.getSelectExtension().setThemeListener()
        selectExtensionDark = fastAdapterDark.getSelectExtension().setThemeListener()
        val enumConstants = Themes.values()
        itemAdapterLight.set(enumConstants.filter { it.nightMode == AppCompatDelegate.MODE_NIGHT_NO }.map(::ThemeItem))
        itemAdapterDark.set(enumConstants.filter { it.nightMode == AppCompatDelegate.MODE_NIGHT_YES }.map(::ThemeItem))
        isSelectable = false
    }

    private fun SelectExtension<ThemeItem>.setThemeListener(): SelectExtension<ThemeItem> {
        isSelectable = true
        multiSelect = true
        selectionListener = object : ISelectionListener<ThemeItem> {
            override fun onSelectionChanged(item: ThemeItem, selected: Boolean) {
                if (item.theme.nightMode == AppCompatDelegate.MODE_NIGHT_YES) {
                    preferences.darkTheme().set(item.theme)
                } else {
                    preferences.lightTheme().set(item.theme)
                }
                if (!selected) {
                    preferences.nightMode().set(item.theme.nightMode)
                } else if (preferences.nightMode()
                    .get() != AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                ) {
                    preferences.nightMode().set(item.theme.nightMode)
                }
                if ((
                    preferences.nightMode().get() == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM &&
                        item.theme.nightMode != context.appDelegateNightMode()
                    ) ||
                    (!selected && item.theme.nightMode == context.appDelegateNightMode())
                ) {
                    fastAdapterLight.notifyDataSetChanged()
                    fastAdapterDark.notifyDataSetChanged()
                } else {
                    activity?.recreate()
                }
            }
        }
        return this
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        binding = ThemesPreferenceBinding.bind(holder.itemView)

        binding.themeRecycler.setHasFixedSize(true)
        binding.themeRecycler.layoutManager = managerLight

        binding.themeRecycler.adapter = fastAdapterLight

        binding.themeRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                lastScrollPostionLight =
                    recyclerView.computeHorizontalScrollOffset()
            }
        })

        binding.themeRecyclerDark.setHasFixedSize(true)
        binding.themeRecyclerDark.layoutManager = managerDark

        binding.themeRecyclerDark.adapter = fastAdapterDark

        binding.themeRecyclerDark.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                lastScrollPostionDark =
                    recyclerView.computeHorizontalScrollOffset()
            }
        })

        if (lastScrollPostionLight != null) {
            val lX = lastScrollPostionLight!!
            (binding.themeRecycler.layoutManager as LinearLayoutManager).apply {
                scrollToPositionWithOffset(
                    lX / 110.dpToPx,
                    -lX % 110.dpToPx
                )
            }
            lastScrollPostionLight = binding.themeRecycler.computeHorizontalScrollOffset()
        } else {
            binding.themeRecycler.scrollToPosition(
                max((selectExtensionLight.selections.firstOrNull() ?: 0) - 1, 0)
            )
        }

        if (lastScrollPostionDark != null) {
            val lX = lastScrollPostionDark!!
            (binding.themeRecyclerDark.layoutManager as LinearLayoutManager).apply {
                scrollToPositionWithOffset(
                    lX / 110.dpToPx,
                    -lX % 110.dpToPx
                )
            }
            lastScrollPostionDark = binding.themeRecyclerDark.computeHorizontalScrollOffset()
        } else {
            binding.themeRecyclerDark.scrollToPosition(
                max((selectExtensionDark.selections.firstOrNull() ?: 0) - 1, 0)
            )
        }
    }

    inner class ThemeItem(val theme: Themes) : AbstractItem<FastAdapter.ViewHolder<ThemeItem>>() {

        /** defines the type defining this item. must be unique. preferably an id */
        override val type: Int = R.id.theme_card_view

        /** defines the layout which will be used for this item in the list */
        override val layoutRes: Int = R.layout.theme_item

        override var identifier = theme.hashCode().toLong()

        override fun getViewHolder(v: View): FastAdapter.ViewHolder<ThemeItem> {
            return ViewHolder(v)
        }

        val colors = theme.getColors()

        @Suppress("UNUSED_PARAMETER")
        override var isSelected: Boolean
            get() = when (preferences.nightMode().get()) {
                AppCompatDelegate.MODE_NIGHT_YES -> preferences.darkTheme().get() == theme
                AppCompatDelegate.MODE_NIGHT_NO -> preferences.lightTheme().get() == theme
                else -> preferences.darkTheme().get() == theme ||
                    preferences.lightTheme().get() == theme
            }
            set(value) {}

        inner class ViewHolder(view: View) : FastAdapter.ViewHolder<ThemeItem>(view) {

            val binding = ThemeItemBinding.bind(view)
            override fun bindView(item: ThemeItem, payloads: List<Any>) {
                binding.themeNameText.setText(item.theme.nameRes)

                binding.checkbox.isVisible = item.isSelected
                binding.themeSelected.isInvisible = !item.isSelected

                if (binding.checkbox.isVisible) {
                    val themeMatchesApp = if (context.isInNightMode()) {
                        item.theme.nightMode == AppCompatDelegate.MODE_NIGHT_YES
                    } else {
                        item.theme.nightMode == AppCompatDelegate.MODE_NIGHT_NO
                    }
                    binding.themeSelected.alpha = if (themeMatchesApp) 1f else 0.5f
                    binding.checkbox.alpha = if (themeMatchesApp) 1f else 0.5f
                }
                binding.themeToolbar.setBackgroundColor(item.colors.appBar)
                binding.themeAppBarText.imageTintList =
                    ColorStateList.valueOf(item.colors.appBarText)
                binding.themeHeroImage.imageTintList =
                    ColorStateList.valueOf(item.colors.primaryText)
                binding.themePrimaryText.imageTintList =
                    ColorStateList.valueOf(item.colors.primaryText)
                binding.themeAccentedButton.imageTintList =
                    ColorStateList.valueOf(item.colors.colorAccent)
                binding.themeSecondaryText.imageTintList =
                    ColorStateList.valueOf(item.colors.secondaryText)
                binding.themeSecondaryText2.imageTintList =
                    ColorStateList.valueOf(item.colors.secondaryText)

                binding.themeBottomBar.setBackgroundColor(item.colors.bottomBar)
                binding.themeItem1.imageTintList =
                    ColorStateList.valueOf(item.colors.inactiveTab)
                binding.themeItem2.imageTintList = ColorStateList.valueOf(item.colors.activeTab)
                binding.themeItem3.imageTintList =
                    ColorStateList.valueOf(item.colors.inactiveTab)
                binding.themeLayout.setBackgroundColor(item.colors.colorBackground)
                if (item.theme.isDarkTheme && preferences.themeDarkAmoled().get()) {
                    binding.themeLayout.setBackgroundColor(Color.BLACK)
                    if (!ThemeUtil.isColoredTheme(item.theme)) {
                        binding.themeBottomBar.setBackgroundColor(Color.BLACK)
                        binding.themeToolbar.setBackgroundColor(Color.BLACK)
                    }
                }
            }

            override fun unbindView(item: ThemeItem) {
            }
        }
    }
}
