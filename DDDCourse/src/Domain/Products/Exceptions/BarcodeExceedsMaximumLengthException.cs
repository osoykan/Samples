using System;

namespace Products.Exceptions
{
    public class BarcodeExceedsMaximumLengthException : Exception
    {
        public BarcodeExceedsMaximumLengthException(string message) : base(message)
        {
        }
    }
}
