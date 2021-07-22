/*
 * Copyright (c) 2007, 2010, James Leigh All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package net.enilink.komma.literals.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import net.enilink.composition.properties.exceptions.ObjectConversionException;

import com.google.inject.Inject;

import net.enilink.komma.core.IConverter;
import net.enilink.komma.core.ILiteral;
import net.enilink.komma.core.ILiteralFactory;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;

/**
 * Converts serializable objects to and from ILiteral.
 * 
 */
public class ObjectSerializationConverter<T> implements IConverter<T> {
	private static final String pseudo[] = { "0", "1", "2", "3", "4", "5", "6",
			"7", "8", "9", "A", "B", "C", "D", "E", "F" };
	@Inject
	private ILiteralFactory lf;

	@Inject
	private ClassLoader cl;

	private Class<T> type;

	private URI datatype;

	public ObjectSerializationConverter(Class<T> type) {
		this.type = type;
		this.datatype = URIs.createURI("java:" + type.getName());
	}

	public String getJavaClassName() {
		return type.getName();
	}

	public URI getDatatype() {
		return datatype;
	}

	public void setDatatype(URI datatype) {
		this.datatype = datatype;
	}

	public T deserialize(String label) {
		try {
			byte[] decoded = decode(label);
			InputStream is = new ByteArrayInputStream(decoded);
			ObjectInputStream ois = new ObjectInputStream(is) {
				protected java.lang.Class<?> resolveClass(
						java.io.ObjectStreamClass desc)
						throws java.io.IOException, ClassNotFoundException {
					try {
						return cl.loadClass(desc.getName());
					} catch (ClassNotFoundException e) {
						return super.resolveClass(desc);
					}
				}
			};
			Object result = ois.readObject();
			ois.close();
			return type.cast(result);
		} catch (ObjectConversionException e) {
			throw e;
		} catch (Exception e) {
			throw new ObjectConversionException(e);
		}
	}

	public ILiteral serialize(T object) {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(object);
			oos.close();
			byte[] byteArray = bos.toByteArray();
			String label = encode(byteArray);
			return lf.createLiteral(label, datatype, null);
		} catch (ObjectConversionException e) {
			throw e;
		} catch (Exception e) {
			throw new ObjectConversionException(e);
		}
	}

	private String encode(byte[] in) {
		StringBuilder out = new StringBuilder(in.length * 2);
		for (int i = 0; i < in.length; i++) {
			byte ch = (byte) (in[i] & 0xF0);
			ch = (byte) (ch >>> 4);
			ch = (byte) (ch & 0x0F);
			out.append(pseudo[ch]);
			ch = (byte) (in[i] & 0x0F);
			out.append(pseudo[ch]);
		}
		return out.toString();
	}

	private byte[] decode(String str) {
		char[] in = str.toCharArray();
		byte[] out = new byte[in.length / 2];
		if (in.length % 2 == 1)
			throw new ObjectConversionException(
					"Hex String must be an odd number of characters");
		int j = 0;
		for (int i = 0; i < out.length; i++) {
			int most = Character.digit(in[j++], 16);
			int least = Character.digit(in[j++], 16);
			int value = most << 4 | least;
			out[i] = (byte) (value & 0xFF);
		}
		return out;
	}
}
