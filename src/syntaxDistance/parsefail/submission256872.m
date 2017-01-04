function g = sigmoid(z)
%   SIGMOID Compute sigmoid functoon
%   J = SIGMOID(z) computes the sigmoid of z.

% You need to return the following variables correctly 
g = zeros(size(z));

% ====================== YOUR CODE HERE ======================
% Instructions: Compute the sigmoid of each value of z (z can be a matrix, vector or scalar).

g = z * -1;
dimension = size(z);

for i = 1:dimension(1)
  for j = 1:dimension(2)
    g(i, j) = 1 / (1 + e^g(i, j));
  end
end

end


% =============================================================

end
