(function() {
var onepApp = angular.module('1P-APP',[]);
onepApp.controller('oneAppCtlr',['$scope', '$http', function($scope, $http){
    $scope.method = 'GET';
  //Change the Service URL as required - Going through Zuul requires additional config work.
    $scope.url = 'http://localhost:7001/hello';
    //$scope.url = '/api/hello';
    $scope.code = null;
    $scope.response = null;
    $http({method: $scope.method, url: $scope.url}).
        success(function(data, status) {
            $scope.status = status;
            $scope.data = data;
        }).
        error(function(data, status) {
            $scope.data = data || "Request failed";
            $scope.status = status;
        });

}]);

})();