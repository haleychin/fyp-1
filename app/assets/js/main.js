$(document).ready(function() {
  $('.grade').tooltip({})
  $('[data-toggle="popover"]').popover({
    trigger: 'hover',
    html: true
  })

  $('#example').DataTable();
  $('.dataTables_wrapper').find('label').each(function() {
    $(this).parent().append($(this).children());
  });
  $('.dataTables_filter').find('input').each(function() {
    $('input').attr("placeholder", "Search");
    $('input').removeClass('form-control-sm');
  });
  $('.dataTables_length').addClass('d-flex flex-row');
  $('.dataTables_filter').addClass('md-form');
  $('select').addClass('mdb-select');
  $('.mdb-select').material_select();
  $('.mdb-select').removeClass('form-control form-control-sm');
  $('.dataTables_filter').find('label').remove();
})

