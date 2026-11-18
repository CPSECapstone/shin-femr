(function () {
    function initializeInventoryImportPreview() {
        var form = document.getElementById('inventoryImportPreviewForm');
        if (!form) return;

        function updateField(input, valid, message) {
            input.classList.toggle('inventory-import-preview__invalid', !valid);
            input.setAttribute('aria-invalid', String(!valid));
            var error = input.closest('td').querySelector('[data-preview-field-error]');
            error.textContent = valid ? '' : message;
            error.style.display = valid ? 'none' : 'block';
        }

        function validateRow(row) {
            var medication = row.querySelector('.preview-medication');
            var quantity = row.querySelector('.preview-quantity');
            // Match the server's signed Java integer range; negative values remain allowed.
            var number = Number(quantity.value);
            var quantityValid = /^[+-]?[0-9]+$/.test(quantity.value)
                    && number >= -2147483648 && number <= 2147483647;
            var medicationValid = medication.value.length > 0;
            updateField(medication, medicationValid, 'Medication name is required.');
            updateField(quantity, quantityValid, 'Please enter a valid whole number');
            // A save failure cannot be cleared by checking the field values in the browser.
            var saveError = row.querySelector('[data-preview-submit-error]');
            row.classList.toggle('inventory-import-preview__error-row',
                    !medicationValid || !quantityValid || !!saveError);
        }

        form.querySelectorAll('[data-preview-row]').forEach(function (row) {
            row.querySelectorAll('.preview-medication, .preview-quantity').forEach(function (input) {
                input.addEventListener('input', function () { validateRow(row); });
            });
        });

        form.querySelectorAll('.preview-remove').forEach(function (button) {
            button.addEventListener('click', function () {
                var row = button.closest('[data-preview-row]');
                var input = row.querySelector('.preview-removed-value');
                var removed = input.value !== 'true';
                input.value = removed ? 'true' : 'false';
                row.classList.toggle('inventory-import-preview__removed', removed);
                button.textContent = removed ? 'Restore' : 'Remove';
                var hasRows = Array.from(form.querySelectorAll('.preview-removed-value'))
                        .some(function (field) { return field.value !== 'true'; });
                if (!hasRows) {
                    document.getElementById('inventoryImportCancelForm').requestSubmit();
                }
            });
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initializeInventoryImportPreview);
    } else {
        initializeInventoryImportPreview();
    }
}());
