package com.splendo.kaluga.hud

import platform.CoreGraphics.CGFloat
import platform.UIKit.*
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.dispatch_after
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time

/*

Copyright 2019 Splendo Consulting B.V. The Netherlands

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

*/

class IOSHUD private constructor(private val view: DefaultView, private val controller: UIViewController) : HUD {

    private class DefaultView(
        private val style: HUD.Style,
        private val titleString: String?
    ) : UIViewController(null, null) {

        lateinit var titleLabel: UILabel

        private val backgroundColor: UIColor
            get() = when (style) {
                HUD.Style.CUSTOM -> UIColor.colorNamed("li_colorBackground") ?: UIColor.lightGrayColor
                HUD.Style.SYSTEM ->
                    if (traitCollection.userInterfaceStyle == UIUserInterfaceStyle.UIUserInterfaceStyleDark)
                        UIColor.blackColor else UIColor.whiteColor
            }

        private val foregroundColor: UIColor
            get() = when (style) {
                HUD.Style.CUSTOM -> UIColor.colorNamed("li_colorAccent") ?: UIColor.darkGrayColor
                HUD.Style.SYSTEM ->
                    if (traitCollection.userInterfaceStyle == UIUserInterfaceStyle.UIUserInterfaceStyleDark)
                        UIColor.whiteColor else UIColor.blackColor
            }

        override fun viewDidLoad() {
            super.viewDidLoad()
            setupView()
        }

        // NOTES: Cast to CGFloat is needed for Arm32
        private fun setupView() {
            // Dimmed background
            view.backgroundColor = UIColor(0.0 as CGFloat, (1 / 3.0) as CGFloat)
            // Main HUD view is stack view
            val stackView = UIStackView().apply {
                axis = UILayoutConstraintAxisVertical
                alignment = UIStackViewAlignmentCenter
                distribution = UIStackViewDistributionFill
                spacing = 8.0 as CGFloat
                translatesAutoresizingMaskIntoConstraints = false
            }
            view.addSubview(stackView)
            // Stack view background view
            val contentView = UIView().apply {
                backgroundColor = this@DefaultView.backgroundColor
                layer.cornerRadius = 14.0 as CGFloat
                translatesAutoresizingMaskIntoConstraints = false
            }
            stackView.addSubview(contentView)
            NSLayoutConstraint.activateConstraints(
                listOf(
                    contentView.leadingAnchor.constraintEqualToAnchor(stackView.leadingAnchor, -32.0 as CGFloat),
                    contentView.trailingAnchor.constraintEqualToAnchor(stackView.trailingAnchor, 32.0 as CGFloat),
                    contentView.topAnchor.constraintEqualToAnchor(stackView.topAnchor, -32.0 as CGFloat),
                    contentView.bottomAnchor.constraintEqualToAnchor(stackView.bottomAnchor, 32.0 as CGFloat),
                    stackView.centerXAnchor.constraintEqualToAnchor(view.centerXAnchor),
                    stackView.centerYAnchor.constraintEqualToAnchor(view.centerYAnchor),
                    stackView.widthAnchor.constraintGreaterThanOrEqualToConstant(64.0 as CGFloat),
                    stackView.widthAnchor.constraintLessThanOrEqualToAnchor(view.widthAnchor, 0.5 as CGFloat),
                    stackView.heightAnchor.constraintGreaterThanOrEqualToConstant(64.0 as CGFloat),
                    stackView.heightAnchor.constraintLessThanOrEqualToAnchor(view.heightAnchor, 0.5 as CGFloat)
                )
            )
            // Activity indicator
            UIActivityIndicatorView(UIActivityIndicatorViewStyleWhiteLarge).apply {
                color = foregroundColor
                startAnimating()
            }.also {
                stackView.addArrangedSubview(it)
            }
            // Place title label
            titleLabel = UILabel().apply {
                text = titleString
                hidden = titleString?.isEmpty() ?: true
                numberOfLines = 0 // Multiline support
            }.also {
                stackView.addArrangedSubview(it)
            }
        }
    }

    class Builder(private val controller: UIViewController) : HUD.Builder {
        override var title: String? = null
        override var style = HUD.Style.SYSTEM
        override fun create() = IOSHUD(DefaultView(style, title), controller)
    }

    init {
        view.apply {
            modalPresentationStyle = UIModalPresentationOverFullScreen
            modalTransitionStyle = UIModalTransitionStyleCrossDissolve
        }
    }

    override val isVisible get() = view.presentingViewController != null

    override fun present(animated: Boolean, completion: () -> Unit): HUD = apply {
        if (controller.presentedViewController != null) {
            controller.dismissViewControllerAnimated(animated) {
                controller.presentViewController(view, animated, completion)
            }
        } else {
            controller.presentViewController(view, animated, completion)
        }
    }

    override fun dismiss(animated: Boolean, completion: () -> Unit) {
        if (controller.presentedViewController != null) {
            controller.dismissViewControllerAnimated(animated, completion)
        } else {
            completion()
        }
    }

    override fun dismissAfter(timeMillis: Long, animated: Boolean) = apply {
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, timeMillis * 1_000_000), dispatch_get_main_queue()) {
            dismiss(animated)
        }
    }

    override fun setTitle(title: String?) {
        view.titleLabel.text = title
        view.titleLabel.hidden = title?.isEmpty() ?: true
    }
}
