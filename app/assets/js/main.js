$(document).ready(function() {
  $('.grade').tooltip({})
  $('[data-toggle="popover"]').popover({
    trigger: 'hover',
    html: true
  })

  $('.sortable').DataTable();
  $('.dataTables_wrapper').find('label').each(function() {
    $(this).parent().append($(this).children());
  });
  $('.dataTables_filter').find('input').each(function(i, item) {
    $(item).attr("placeholder", "Search");
    $(item).removeClass('form-control-sm');
  });
  $('.dataTables_length').addClass('d-flex flex-row');
  $('.dataTables_filter').addClass('md-form');
  $('select').addClass('mdb-select');
  $('.mdb-select').material_select();
  $('.mdb-select').removeClass('form-control form-control-sm');
  $('.dataTables_filter').find('label').remove();

  var programmeSelection = $("#programmeSelection");
  var intakeSelection = $("#intakeSelection");
  var filterBtn = $("#filterBtn");

  filterBtn.on("click", function(event) {
    event.preventDefault();
    var prog = programmeSelection.val();
    var intake = intakeSelection.val();

    var query = "?";
    if (prog !== "") {
      query += "programme=" + prog + "&";
    }

    if (intake !== "") {
      query += "intake=" + intake;
    }

    window.location.href = this.href + query;
  })
})

