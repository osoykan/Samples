using System;

namespace Products.Exceptions
{
    public class AggregateDuplicatedException : Exception
    {
        public AggregateDuplicatedException(string message) : base(message)
        {
        }
    }
}
