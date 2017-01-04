function [J, grad] = costFunction(theta, X, y)
%COSTFUNCTION Compute cost and gradient for logistic regression
%   J = COSTFUNCTION(theta, X, y) computes the cost of using theta as the
%   parameter for logistic regression and the gradient of the cost
%   w.r.t. to the parameters.

% Initialize some useful values
m = length(y); % number of training examples

% You need to return the following variables correctly 
J = 0;
grad = zeros(size(theta));

% ====================== YOUR CODE HERE ======================
% Instructions: Compute the cost of a particular choice of theta.
%               You should set J to the cost.
%               Compute the partial derivatives and set grad to the partial
%               derivatives of the cost w.r.t. each parameter in theta
%
% Note: grad should have the same dimensions as theta
%

J = (y * -log(sigmoid(theta'*X')) + (1-y)*-log(1 - sigmoid(theta'*X')));
J = J/m;


h = sigmoid(X * theta); 
p = 1 - y;
for i = 1:s j1 = -y .* log(h(1:m,i)) q = log(1 - (h(1:m,i)) ) j2 = -p .* q J = (1/m) * sum(j1+j2)
t = (h(1:m,i) .- y ) .* X(:,i);
grad(i,1) = (1/m) * sum(t);
end

% =============================================================

end
