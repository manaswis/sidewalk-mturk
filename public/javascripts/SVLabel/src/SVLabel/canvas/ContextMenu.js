function ContextMenu (uiContextMenu) {
    var self = { className: "ContextMenu" },
        status = {
            targetLabel: null,
            visibility: 'hidden'
        };
    var $menuWindow = uiContextMenu.holder,
        $connector = uiContextMenu.connector,
        $radioButtons = uiContextMenu.radioButtons,
        $temporaryProblemCheckbox = uiContextMenu.temporaryProblemCheckbox,
        $descriptionTextBox = uiContextMenu.textBox,
        windowWidth = $menuWindow.width();
    var $OKButton = $menuWindow.find("#context-menu-ok-button");
    var $radioButtonLabels = $menuWindow.find(".radio-button-labels");


    document.addEventListener("mousedown", hide);
    $menuWindow.on('mousedown', handleMenuWindowMouseDown);
    $radioButtons.on('change', _handleRadioChange);
    $temporaryProblemCheckbox.on('change', handleTemporaryProblemCheckboxChange);
    $descriptionTextBox.on('change', handleDescriptionTextBoxChange);
    $descriptionTextBox.on('focus', handleDescriptionTextBoxFocus);
    $descriptionTextBox.on('blur', handleDescriptionTextBoxBlur);
    uiContextMenu.closeButton.on('click', handleCloseButtonClick);
    $OKButton.on('click', _handleOKButtonClick);
    $radioButtonLabels.on('mouseenter', _handleRadioButtonLabelMouseEnter);
    $radioButtonLabels.on('mouseleave', _handleRadioButtonLabelMouseLeave);

    function checkRadioButton (value) {
        uiContextMenu.radioButtons.filter(function(){return this.value==value}).prop("checked", true).trigger("click");
    }

    /**
     * Returns a status
     * @param key
     * @returns {null}
     */
    function getStatus (key) {
        return (key in status) ? status[key] : null;
    }

    /**
     * Get the current target label
     * @returns {null}
     */
    function getTargetLabel () {
        return getStatus('targetLabel');
    }

    /**
     * Combined with document.addEventListener("mousedown", hide), this method will close the context menu window
     * when user clicks somewhere on the window except for the area on the context menu window.
     * @param e
     */
    function handleMenuWindowMouseDown (e) {
        e.stopPropagation();
    }

    function handleDescriptionTextBoxChange(e) {
        var description = $(this).val(),
            label = getTargetLabel();
        if (label) {
            label.setProperty('description', description);
        }
    }

    function handleDescriptionTextBoxBlur() {
        svl.tracker.push('ContextMenu_TextBoxBlur');
        svl.ribbon.enableModeSwitch();
        svl.keyboard.setStatus('focusOnTextField', false);
    }

    function handleDescriptionTextBoxFocus() {
        svl.tracker.push('ContextMenu_TextBoxFocus');
        svl.ribbon.disableModeSwitch();
        svl.keyboard.setStatus('focusOnTextField', true);
    }

    function handleCloseButtonClick () {
        svl.tracker.push('ContextMenu_CloseButtonClick');
        hide();
    }

    function _handleOKButtonClick () {
        svl.tracker.push('ContextMenu_OKButtonClick');
        hide();
    }

    /**
     *
     * @param e
     */
    function _handleRadioChange (e) {
        var severity = parseInt($(this).val(), 10);
        var label = getTargetLabel();
        svl.tracker.push('ContextMenu_RadioChange', { LabelType: label.getProperty("labelType"), RadioValue: severity });

        self.updateRadioButtonImages();

        if (label) {
            label.setProperty('severity', severity);
        }
    }

    function _handleRadioButtonLabelMouseEnter () {
        var radioValue = parseInt($(this).find("input").attr("value"), 10);
        self.updateRadioButtonImages(radioValue);
    }

    function _handleRadioButtonLabelMouseLeave () {
        self.updateRadioButtonImages();
    }

    self.updateRadioButtonImages = function (hoveredRadioButtonValue) {
        var $radioButtonImages = $radioButtonLabels.find("input + img");
        var $selectedRadioButtonImage;
        var $hoveredRadioButtonImage;
        var imageURL;

        $radioButtonImages.each(function (i, element) {
            imageURL = $(element).attr("default-src");
            $(element).attr("src", imageURL);
        });

        // Update the hovered radio button image
        if (hoveredRadioButtonValue && hoveredRadioButtonValue > 0 && hoveredRadioButtonValue <= 5) {
            $hoveredRadioButtonImage = $radioButtonLabels.find("input[value='" + hoveredRadioButtonValue + "'] + img");
            imageURL = $hoveredRadioButtonImage.attr("default-src");
            imageURL = imageURL.replace("_BW.png", ".png");
            $hoveredRadioButtonImage.attr("src", imageURL);
        }

        // Update the selected radio button image
        $selectedRadioButtonImage = $radioButtonLabels.find("input:checked + img");
        if ($selectedRadioButtonImage.length > 0) {
            imageURL = $selectedRadioButtonImage.attr("default-src");
            imageURL = imageURL.replace("_BW.png", ".png");
            $selectedRadioButtonImage.attr("src", imageURL);
        }
    };


    /**
     *
     * @param e
     */
    function handleTemporaryProblemCheckboxChange (e) {
        var checked = $(this).is(":checked"),
            label = getTargetLabel();
        svl.tracker.push('ContextMenu_CheckboxChange', { checked: checked });

        if (label) {
            label.setProperty('temporaryProblem', checked);
        }
    }

    /**
     * Hide the context menu
     * @returns {hide}
     */
    function hide () {
        $menuWindow.css('visibility', 'hidden');
        setBorderColor('black');
        setStatus('visibility', 'hidden');
        return this;
    }

    /**
     * Checks if the menu is open or not
     * @returns {boolean}
     */
    function isOpen() {
        return getStatus('visibility') == 'visible';
    }

    /**
     * Set the border color of the menu window
     * @param color
     */
    function setBorderColor(color) {
        $menuWindow.css('border-color', color);
        $connector.css('background-color', color);
    }

    /**
     * Sets a status
     * @param key
     * @param value
     * @returns {setStatus}
     */
    function setStatus (key, value) {
        status[key] = value;
        return this;
    }

    /**
     * Show the context menu
     * @param x x-coordinate on the canvas pane
     * @param y y-coordinate on the canvas pane
     * @param param a parameter object
     */
    function show (x, y, param) {

        setStatus('targetLabel', null);
        $radioButtons.prop('checked', false);
        $temporaryProblemCheckbox.prop('checked', false);
        $descriptionTextBox.val(null);
        if (x && y && ('targetLabel' in param)) {
            var labelType = param.targetLabel.getLabelType(),
                acceptedLabelTypes = ['SurfaceProblem', 'Obstacle', 'NoCurbRamp', 'Other', 'CurbRamp'];
            if (acceptedLabelTypes.indexOf(labelType) != -1) {
                setStatus('targetLabel', param.targetLabel);
                $menuWindow.css({
                    visibility: 'visible',
                    left: x - windowWidth / 2,
                    top: y + 20
                });

                if (param) {
                    if ('targetLabelColor' in param) { setBorderColor(param.targetLabelColor); }
                }
                setStatus('visibility', 'visible');

                // Set the menu value if label has it's value set.
                var severity = param.targetLabel.getProperty('severity'),
                    temporaryProblem = param.targetLabel.getProperty('temporaryProblem'),
                    description = param.targetLabel.getProperty('description');
                if (severity) {
                    $radioButtons.each(function (i, v) {
                       if (severity == i + 1) { $(this).prop("checked", true); }
                    });
                }

                if (temporaryProblem) {
                    $temporaryProblemCheckbox.prop("checked", temporaryProblem);
                }

                if (description) {
                    $descriptionTextBox.val(description);
                } else {
                    var example = '', defaultText = "Description";
                    if (labelType == 'CurbRamp') {
                        example = " (e.g., narrow curb ramp)";
                    } else if (labelType == 'NoCurbRamp') {
                        example = " (e.g., unclear if a curb ramp is needed)";
                    } else if (labelType == 'Obstacle') {
                        example = " (e.g., light pole blocking sidewalk)";
                    } else if (labelType == 'SurfaceProblem') {
                        example = " (e.g., unleveled due to a tree root)";
                    }
                    $descriptionTextBox.prop("placeholder", defaultText + example);
                }
            }
        }
        self.updateRadioButtonImages();
    }

    self.checkRadioButton = checkRadioButton;
    self.getTargetLabel = getTargetLabel;
    self.hide = hide;
    self.isOpen = isOpen;
    self.show = show;
    return self;
}