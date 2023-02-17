/*
 * Copyright 2022 Stax
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hover.stax.presentation.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import com.hover.sdk.actions.HoverAction
import com.hover.stax.R
import com.hover.stax.addAccounts.AddAccountActivity
import com.hover.stax.addAccounts.AddAccountContract
import com.hover.stax.addAccounts.AddAccountViewModel
import com.hover.stax.databinding.FragmentHomeBinding
import com.hover.stax.domain.model.USSDAccount
import com.hover.stax.home.MainActivity
import com.hover.stax.hover.AbstractBalanceCheckerFragment
import com.hover.stax.utils.AnalyticsUtil
import com.hover.stax.utils.NavUtil
import com.hover.stax.utils.UIHelper
import com.hover.stax.utils.Utils
import com.hover.stax.utils.collectLifecycleFlow
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class HomeFragment : AbstractBalanceCheckerFragment(), FinancialTipClickInterface {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val balancesViewModel: BalancesViewModel by sharedViewModel()
    private val homeViewModel: HomeViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        AnalyticsUtil.logAnalyticsEvent(getString(R.string.visit_screen, getString(R.string.visit_home)), requireContext())
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setComposeView()

        observeForBalances()
        observeForBonus()
    }

    private fun getHomeClickFunctions(): HomeClickFunctions {
        fun onSendMoneyClicked() = navigateTo(getTransferDirection(HoverAction.P2P))
        fun onBuyAirtimeClicked() = navigateTo(getTransferDirection(HoverAction.AIRTIME))
        fun onBuyGoodsClicked() = navigateTo(HomeFragmentDirections.actionNavigationHomeToMerchantFragment())
        fun onPayBillClicked() = navigateTo(HomeFragmentDirections.actionNavigationHomeToPaybillFragment())
        fun onRequestMoneyClicked() = navigateTo(HomeFragmentDirections.actionNavigationHomeToNavigationRequest())
        fun onClickedAddNewAccount() = goToAddAccount()
        fun onClickedTermsAndConditions() = Utils.openUrl(getString(R.string.terms_and_condition_url), requireContext())
        fun onClickedSettingsIcon() = navigateTo(HomeFragmentDirections.toSettingsFragment())
        fun onClickedRewards() = navigateTo(HomeFragmentDirections.actionGlobalRewardsFragment())

        return HomeClickFunctions(
            onSendMoneyClicked = { onSendMoneyClicked() },
            onBuyAirtimeClicked = { onBuyAirtimeClicked() },
            onBuyGoodsClicked = { onBuyGoodsClicked() },
            onPayBillClicked = { onPayBillClicked() },
            onRequestMoneyClicked = { onRequestMoneyClicked() },
            onClickedAddNewAccount = { onClickedAddNewAccount() },
            onClickedTC = { onClickedTermsAndConditions() },
            onClickedSettingsIcon = { onClickedSettingsIcon() },
            onClickedRewards = { onClickedRewards() }
        )
    }

	val addAccount = registerForActivityResult(AddAccountContract()) { data: Intent? ->

	}

    private fun goToAddAccount() {
	    addAccount.launch(null)
    }

    private fun setComposeView() {
        binding.root.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        binding.root.setContent {
            HomeScreen(getHomeClickFunctions(), homeViewModel, navTo = { dest -> navigateTo(dest) })
        }
    }

    private fun observeForBonus() {
        if (homeViewModel.bonusActions.value?.isNotEmpty() == true)
            navigateTo(getTransferDirection(HoverAction.AIRTIME, homeViewModel.bonusActions.value?.first()?.from_institution_id.toString()))
    }

    private fun observeForBalances() {
        collectLifecycleFlow(balancesViewModel.balanceAction) {
            attemptCallHover(balancesViewModel.userRequestedBalanceAccount.value, it)
        }

//        collectLifecycleFlow(channelsViewModel.accountCallback) {
//            askToCheckBalance(it)
//        }

        collectLifecycleFlow(balancesViewModel.actionRunError) {
            UIHelper.flashAndReportError(requireActivity(), it)
        }
    }

    private fun getTransferDirection(type: String, institutionId: String? = null): NavDirections {
        return HomeFragmentDirections.actionNavigationHomeToNavigationTransfer(type).also {
            if (institutionId != null) it.institutionId = institutionId
        }
    }

    private fun attemptCallHover(account: USSDAccount?, action: HoverAction?) {
        action?.let { account?.let { callHover(checkBalance, generateSessionBuilder(account, action)) } }
    }

//    private fun askToCheckBalance(account: USSDAccount) {
//        val dialog = StaxDialog(requireActivity()).setDialogTitle(R.string.check_balance_title)
//            .setDialogMessage(R.string.check_balance_desc).setNegButton(R.string.later, null)
//            .setPosButton(R.string.check_balance_title) { onTapBalanceRefresh(account) }
//        dialog.showIt()
//    }

    private fun navigateTo(navDirections: NavDirections) = (requireActivity() as MainActivity).checkPermissionsAndNavigate(navDirections)

    private fun navigateTo(dest: Int) = findNavController().navigate(dest)

    override fun onTipClicked(tipId: String?) {
        val destination = HomeFragmentDirections.actionNavigationHomeToWellnessFragment().apply { setTipId(tipId) }
        NavUtil.navigate(findNavController(), destination)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}