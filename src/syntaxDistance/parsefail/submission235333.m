function [J, grad] = costFunctionReg(theta, X, y, lambda)
%COSTFUNCTIONREG Compute cost and gradient for logistic regression with regularization
%   J = COSTFUNCTIONREG(theta, X, y, lambda) computes the cost of using
%   theta as the parameter for regularized logistic regression and the
%   gradient of the cost w.r.t. to the parameters. 

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

one = ones(size(y));
rm = 1/m; % reciprocal m

t = theta;
t(1) = 0; % ignore the first term in the sum of theta^2
J = rm*(-y'*log(sigmoid(X*theta))-(one-y)'*log((one-sigmoid(X*theta))) + (lambda./2)*t'*t);

grad(1) = rm*(sigmoid(X*theta) - y)'*X(:,1); % compute gradient for theta0 w/o the regularization term
for j = 2:size(theta),
	grad(j) = rm*((sigmoid(X*theta) - y)'*X(:,j) + lambda*theta(j));
end

% =============================================================

end



% =============================================================

end
