(function () {
    function initializeInventoryImportPreview() {
        var form = document.getElementById('inventoryImportPreviewForm');
        if (!form) return;

        form.querySelectorAll('.preview-remove').forEach(function (button) {
            button.addEventListener('click', function () {
                var row = button.closest('[data-preview-row]');
                var input = row.querySelector('.preview-removed-value');
                var removed = input.value !== 'true';
                input.value = removed ? 'true' : 'false';
                row.classList.toggle('inventory-import-preview__removed', removed);
                button.textContent = removed ? 'Restore' : 'Remove';
            });
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initializeInventoryImportPreview);
    } else {
        initializeInventoryImportPreview();
    }
}());


/* Code that could be useful if we re-implement on-edit validation again*/
// var message = form.querySelector('.inventory-import-preview__client-error');
//
// function validate() {
//     var names = {};
//     var invalid = false;
//
//     form.querySelectorAll('[data-preview-row]').forEach(function (row) {
//         var removed = row.querySelector('.preview-removed-value').value === 'true';
//         var name = row.querySelector('.preview-medication');
//         var quantity = row.querySelector('.preview-quantity');
//         name.classList.remove('inventory-import-preview__invalid');
//         quantity.classList.remove('inventory-import-preview__invalid');
//
//         if (!removed && (!name.value.trim() || names[name.value.trim()])) {
//             name.classList.add('inventory-import-preview__invalid');
//             if (names[name.value.trim()]) names[name.value.trim()].classList.add('inventory-import-preview__invalid');
//             invalid = true;
//         }
//         if (!removed) names[name.value.trim()] = name;
//
//         if (!removed && (!/^-?\d+$/.test(quantity.value) || Number(quantity.value) < 0)) {
//             quantity.classList.add('inventory-import-preview__invalid');
//             invalid = true;
//         }
//     });
//
//     message.textContent = invalid ? 'Medication names must be unique and final quantities cannot be negative.' : '';
//     return !invalid;
// }
//
// function inventoryByName() {
//     var values = {};
//     document.querySelectorAll('#inventoryTable tbody tr').forEach(function (row) {
//         var name = row.querySelector('.name');
//         var current = row.querySelector('.editCurrentQuantity');
//         var initial = row.querySelector('.editTotalQuantity');
//         if (name && current && initial) {
//             values[name.textContent.trim()] = {
//                 current: current.textContent.trim(),
//                 initial: initial.textContent.trim()
//             };
//         }
//     });
//     return values;
// }
//
// function recalculateForMedication(row) {
//     var target = inventoryByName()[row.querySelector('.preview-medication').value.trim()];
//     var delta = Number(row.getAttribute('data-delta'));
//     var cells = row.children;
//     var quantity = row.querySelector('.preview-quantity');
//
//     if (target && !isNaN(delta)) {
//         cells[1].textContent = 'Changed';
//         cells[2].textContent = target.current;
//         quantity.value = Number(target.current) + delta;
//         cells[4].textContent = target.initial;
//     } else if (!target && !isNaN(delta)) {
//         cells[1].textContent = 'New uploaded quantity';
//         cells[2].textContent = '--';
//         quantity.value = delta;
//         cells[4].textContent = delta;
//     }
// }
//
// form.addEventListener('input', validate);
// form.querySelectorAll('.preview-medication').forEach(function (input) {
//     input.addEventListener('change', function () {
//         recalculateForMedication(input.closest('[data-preview-row]'));
//         validate();
//     });
// });
// form.addEventListener('submit', function (event) {
//     if (!validate()) event.preventDefault();
// });
